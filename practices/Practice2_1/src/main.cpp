/**
 * Практика 2.1 — Win32 + системный лоток (notification area).
 * Требования: иконка в трее, ЛКМ — окно, ПКМ — меню Открыть/Выход,
 * TaskbarCreated, запуск без окна (--background / -hidden), закрытие окна —
 * приложение продолжает работу в трее, Файл → Выход завершает процесс,
 * один экземпляр (named mutex).
 */
#include <windows.h>
#include <shellapi.h>

#include <string>

#pragma comment(lib, "Shell32.lib")

namespace {

constexpr UINT_PTR kTrayNotifyId = 1;
constexpr UINT WM_APP_TRAY = WM_APP + 50;

HWND g_hwnd{};
NOTIFYICONDATAW g_nid{};
HMENU g_menu_bar{};
HMENU g_file_menu{};
bool g_tray_registered = false;
UINT g_taskbar_created_msg = 0;

void RemoveTrayIcon() {
    if (!g_tray_registered) return;
    Shell_NotifyIconW(NIM_DELETE, &g_nid);
    g_tray_registered = false;
}

void AddTrayIcon() {
    RemoveTrayIcon();
    ZeroMemory(&g_nid, sizeof(g_nid));
    g_nid.cbSize = sizeof(NOTIFYICONDATAW);
    g_nid.hWnd = g_hwnd;
    g_nid.uID = static_cast<UINT>(kTrayNotifyId);
    g_nid.uFlags = NIF_MESSAGE | NIF_ICON | NIF_TIP | NIF_SHOWTIP;
    g_nid.uCallbackMessage = WM_APP_TRAY;
    g_nid.hIcon = reinterpret_cast<HICON>(
        LoadImageW(nullptr, IDI_APPLICATION, IMAGE_ICON,
                   GetSystemMetrics(SM_CXSMICON), GetSystemMetrics(SM_CYSMICON),
                   LR_SHARED));

    WCHAR tip[] = L"Практика 2.1 — трей";
    CopyMemory(g_nid.szTip, tip, sizeof(tip));

    if (Shell_NotifyIconW(NIM_ADD, &g_nid)) {
        g_tray_registered = true;
        g_nid.uVersion = NOTIFYICON_VERSION_4;
        Shell_NotifyIconW(NIM_SETVERSION, &g_nid);
    }
}

void ShowMainWindow() {
    if (!g_hwnd) return;
    ShowWindow(g_hwnd, SW_SHOW);
    SetForegroundWindow(g_hwnd);
    BringWindowToTop(g_hwnd);
}

void CleanupMenus(HWND hwnd) {
    if (hwnd) SetMenu(hwnd, nullptr);
    if (g_menu_bar) {
        DestroyMenu(g_menu_bar);
        g_menu_bar = nullptr;
        g_file_menu = nullptr;
    } else if (g_file_menu) {
        DestroyMenu(g_file_menu);
        g_file_menu = nullptr;
    }
}

void TrackTrayContextMenu(POINT pt) {
    HMENU popup = CreatePopupMenu();
    if (!popup) return;

    AppendMenuW(popup, MF_STRING, 1001, L"Открыть");
    AppendMenuW(popup, MF_STRING, 1002, L"Выход");

    SetForegroundWindow(g_hwnd);
    const UINT flags = TPM_RIGHTBUTTON | TPM_BOTTOMALIGN | TPM_LEFTALIGN;
    TrackPopupMenu(popup, flags, pt.x, pt.y, 0, g_hwnd, nullptr);
    DestroyMenu(popup);
}

bool ParseHiddenLaunchFlag() {
    int argc = 0;
    LPWSTR* argv = CommandLineToArgvW(GetCommandLineW(), &argc);
    if (!argv) return false;
    bool hidden = false;
    for (int i = 1; i < argc; ++i) {
        const std::wstring a(argv[i]);
        if (a == L"--background" || a == L"-background" ||
            a == L"--hidden" || a == L"-hidden") {
            hidden = true;
            break;
        }
    }
    LocalFree(argv);
    return hidden;
}

LRESULT CALLBACK MainWndProc(HWND hwnd, UINT msg, WPARAM wParam,
                             LPARAM lParam) {
    if (msg != 0 && msg == g_taskbar_created_msg && g_taskbar_created_msg != 0) {
        AddTrayIcon();
        return 0;
    }

    switch (msg) {
        case WM_CREATE: {
            g_menu_bar = CreateMenu();
            g_file_menu = CreateMenu();
            AppendMenuW(g_file_menu, MF_STRING, 2002, L"Выход");
            AppendMenuW(g_menu_bar, MF_POPUP,
                        reinterpret_cast<UINT_PTR>(g_file_menu), L"Файл");
            SetMenu(hwnd, g_menu_bar);
            DrawMenuBar(hwnd);
            return 0;
        }
        case WM_COMMAND: {
            const UINT id = LOWORD(wParam);
            // Трей: Открыть; трей или меню: Выход → завершение приложения
            if (id == 1001) {
                ShowMainWindow();
                return 0;
            }
            if (id == 1002 || id == 2002) {
                DestroyWindow(hwnd);
                return 0;
            }
            return DefWindowProcW(hwnd, msg, wParam, lParam);
        }
        case WM_APP_TRAY: {
            if (lParam == WM_LBUTTONUP) {
                ShowMainWindow();
                return 0;
            }
            if (lParam == WM_RBUTTONUP) {
                POINT pt{};
                GetCursorPos(&pt);
                TrackTrayContextMenu(pt);
                return 0;
            }
            return 0;
        }
        case WM_CLOSE:
            // Крестик / Alt+F4 — скрываем окно; процесс живёт в трее
            ShowWindow(hwnd, SW_HIDE);
            return 0;
        case WM_DESTROY:
            RemoveTrayIcon();
            CleanupMenus(hwnd);
            PostQuitMessage(0);
            return 0;
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}

} // namespace

int APIENTRY wWinMain(_In_ HINSTANCE hInst, _In_opt_ HINSTANCE,
                      _In_ LPWSTR, _In_ int) {
    // Один процесс на пользователя; второй экземпляр не доходит до трея
    HANDLE mutex = CreateMutexW(nullptr, TRUE,
                                L"Local\\Practice21_TraySingleton_Mutex");
    if (!mutex) return 2;
    if (GetLastError() == ERROR_ALREADY_EXISTS) {
        CloseHandle(mutex);
        MessageBoxW(nullptr,
                    L"Приложение уже запущено.",
                    L"Практика 2.1",
                    MB_OK | MB_ICONINFORMATION);
        return 1;
    }

    g_taskbar_created_msg = RegisterWindowMessageW(L"TaskbarCreated");

    const WCHAR kClass[] = L"Practice21TrayMainClass";
    WNDCLASSW wc{};
    wc.lpfnWndProc = MainWndProc;
    wc.hInstance = hInst;
    wc.lpszClassName = kClass;
    wc.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    wc.hbrBackground = reinterpret_cast<HBRUSH>(COLOR_WINDOW + 1);
    RegisterClassW(&wc);

    g_hwnd = CreateWindowExW(
        0L, kClass, L"Практика 2.1 — графическое приложение",
        WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, 560, 360,
        nullptr, nullptr, hInst, nullptr);

    const bool start_hidden = ParseHiddenLaunchFlag();
    ShowWindow(g_hwnd, start_hidden ? SW_HIDE : SW_SHOW);

    AddTrayIcon();

    MSG msg{};
    while (GetMessageW(&msg, nullptr, 0, 0) > 0) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    CloseHandle(mutex);
    return static_cast<int>(msg.wParam);
}
