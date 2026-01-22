/**
 * MiniShop Global Navigation & App Switcher
 * Automatically injected into all pages to provide seamless navigation,
 * auth state synchronization, and a quick-access switcher.
 */

(function() {
    // 1. Map of known link keywords to actual filenames
    const linkMap = {
        'login': 'login.html',
        'đăng nhập': 'login.html',
        'register': 'register.html',
        'đăng ký': 'register.html',
        'tạo tài khoản': 'register.html',
        'forgot password': 'forgot-password.html',
        'quên mật khẩu': 'forgot-password.html',
        'change password': 'change-password.html',
        'đổi mật khẩu': 'change-password.html',
        'account': 'account.html',
        'tài khoản': 'account.html',
        'hồ sơ': 'account.html',
        'cart': 'cart.html',
        'giỏ hàng': 'cart.html',
        'shopping_cart': 'cart.html',
        'checkout': 'checkout.html',
        'thanh toán': 'checkout.html',
        'products': 'product-listing.html',
        'sản phẩm': 'product-listing.html',
        'danh sách sản phẩm': 'product-listing.html',
        'shop': 'product-listing.html',
        'product details': 'product-detail.html',
        'chi tiết sản phẩm': 'product-detail.html',
        'my orders': 'my-orders.html',
        'đơn hàng của tôi': 'my-orders.html',
        'order details': 'order-details.html',
        'chi tiết đơn hàng': 'order-details.html',
        'review': 'product-review.html',
        'đánh giá': 'product-review.html',
        'my products': 'my-products.html',
        'sản phẩm của tôi': 'my-products.html',
        'seller center': 'my-products.html',
        'edit product': 'seller-product-edit.html',
        'thêm sản phẩm': 'seller-product-edit.html',
        'inventory management': 'inventory-management.html',
        'quản lý kho': 'inventory-management.html',
        'orders to process': 'orders-to-process.html',
        'xử lý đơn hàng': 'orders-to-process.html',
        'user management': 'admin-users.html',
        'quản lý người dùng': 'admin-users.html',
        'categories': 'category-management.html',
        'danh mục': 'category-management.html',
        'category management': 'category-management.html',
        'global orders': 'global-order-monitoring.html',
        'giám sát đơn hàng': 'global-order-monitoring.html',
        'inventory adjustments': 'admin-inventory.html',
        'điều chỉnh kho': 'admin-inventory.html',
        'payment logs': 'admin-payment-logs.html',
        'payment & callback logs': 'admin-payment-logs.html',
        'nhật ký thanh toán': 'admin-payment-logs.html',
        'notification logs': 'notification-logs.html',
        'nhật ký thông báo': 'notification-logs.html',
        'debug console': 'debug-api-console.html',
        'api console': 'debug-api-console.html',
        'system status': 'debug-system-status.html',
        'trạng thái hệ thống': 'debug-system-status.html',
        'minishop': 'product-listing.html',
        'home': 'product-listing.html',
        'trang chủ': 'product-listing.html'
    };

    // 2. Resolve link helper
    function resolveLink(text, icon) {
        const cleanText = (text || '').trim().toLowerCase();
        const cleanIcon = (icon || '').trim().toLowerCase();

        if (cleanText && linkMap[cleanText]) return linkMap[cleanText];
        if (cleanIcon && linkMap[cleanIcon]) return linkMap[cleanIcon];

        for (const [key, target] of Object.entries(linkMap)) {
            if (cleanText.includes(key)) return target;
        }
        return null;
    }

    // 3. Attach links across document
    function linkifyDocument() {
        // Logo / Brand links
        document.querySelectorAll('header h1, header .font-headline-lg, nav h1, nav .font-headline-md').forEach(el => {
            if (el.textContent.includes('MiniShop')) {
                el.style.cursor = 'pointer';
                el.addEventListener('click', () => window.location.href = 'index.html');
            }
        });

        // <a> tags with href="#" or empty
        document.querySelectorAll('a').forEach(a => {
            const href = a.getAttribute('href');
            if (!href || href === '#' || href === 'javascript:void(0)') {
                const icon = a.querySelector('.material-symbols-outlined')?.textContent;
                const text = a.textContent;
                const target = resolveLink(text, icon);
                if (target) {
                    a.setAttribute('href', target);
                }
            }
        });

        // Buttons with common action names
        document.querySelectorAll('button').forEach(btn => {
            const text = (btn.textContent || '').trim().toLowerCase();
            const icon = btn.querySelector('.material-symbols-outlined')?.textContent?.toLowerCase() || '';

            if (text.includes('thêm vào giỏ') || text.includes('add to cart')) {
                btn.addEventListener('click', (e) => {
                    e.preventDefault();
                    window.location.href = 'cart.html';
                });
            } else if (text.includes('tiến hành thanh toán') || text.includes('checkout') || text.includes('mua hàng')) {
                btn.addEventListener('click', (e) => {
                    e.preventDefault();
                    window.location.href = 'checkout.html';
                });
            } else if (text.includes('new entry') || text.includes('thêm mới')) {
                btn.addEventListener('click', (e) => {
                    e.preventDefault();
                    if (window.location.pathname.includes('seller') || window.location.pathname.includes('product')) {
                        window.location.href = 'seller-product-edit.html';
                    } else if (window.location.pathname.includes('category')) {
                        window.location.href = 'category-management.html';
                    } else {
                        window.location.href = 'admin-inventory.html';
                    }
                });
            } else if (icon === 'account_circle') {
                btn.addEventListener('click', () => window.location.href = 'account.html');
            } else if (icon === 'shopping_cart') {
                btn.addEventListener('click', () => window.location.href = 'cart.html');
            } else if (icon === 'logout') {
                btn.addEventListener('click', () => {
                    localStorage.removeItem('minishop_access_token');
                    localStorage.removeItem('minishop_user');
                    window.location.href = 'login.html';
                });
            }
        });

        // Product Cards
        document.querySelectorAll('.product-card, [data-product-card]').forEach(card => {
            card.style.cursor = 'pointer';
            card.addEventListener('click', (e) => {
                if (!e.target.closest('button') && !e.target.closest('a')) {
                    window.location.href = 'product-detail.html';
                }
            });
        });
    }

    // 4. Inject Floating Quick Navigation Switcher
    function injectQuickSwitcher() {
        if (document.getElementById('minishop-quick-switcher')) return;

        const switcher = document.createElement('div');
        switcher.id = 'minishop-quick-switcher';
        switcher.innerHTML = `
            <style>
                #minishop-quick-switcher {
                    position: fixed;
                    bottom: 16px;
                    right: 16px;
                    z-index: 999999;
                    font-family: 'Inter', system-ui, -apple-system, sans-serif;
                }
                .ms-switcher-btn {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    background: #004ac6;
                    color: white;
                    padding: 10px 16px;
                    border-radius: 9999px;
                    font-size: 13px;
                    font-weight: 600;
                    box-shadow: 0 10px 25px -5px rgba(0, 74, 198, 0.4);
                    cursor: pointer;
                    border: none;
                    transition: all 0.2s ease;
                }
                .ms-switcher-btn:hover {
                    background: #2563eb;
                    transform: translateY(-2px);
                    box-shadow: 0 15px 30px -5px rgba(37, 99, 235, 0.5);
                }
                .ms-switcher-menu {
                    position: absolute;
                    bottom: 54px;
                    right: 0;
                    width: 320px;
                    background: white;
                    border: 1px solid #e0e3e5;
                    border-radius: 16px;
                    box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.15);
                    padding: 16px;
                    display: none;
                    max-height: 520px;
                    overflow-y: auto;
                }
                .ms-switcher-menu.active {
                    display: block;
                    animation: msFadeIn 0.2s ease-out;
                }
                @keyframes msFadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .ms-sec-title {
                    font-size: 11px;
                    font-weight: 700;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                    color: #737686;
                    margin: 12px 0 6px 4px;
                }
                .ms-sec-title:first-child { margin-top: 0; }
                .ms-menu-item {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    padding: 8px 10px;
                    border-radius: 8px;
                    color: #191c1e;
                    text-decoration: none;
                    font-size: 13px;
                    font-weight: 500;
                    transition: background 0.15s;
                }
                .ms-menu-item:hover {
                    background: #f2f4f6;
                    color: #004ac6;
                }
                .ms-menu-item.active {
                    background: #dbe1ff;
                    color: #00174b;
                    font-weight: 600;
                }
                .ms-badge-pulse {
                    width: 8px;
                    height: 8px;
                    background: #10b981;
                    border-radius: 50%;
                    box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
                }
            </style>

            <div class="ms-switcher-menu" id="msSwitcherMenu">
                <div style="display: flex; justify-content: space-between; align-items: center; padding-bottom: 8px; border-bottom: 1px solid #e0e3e5; margin-bottom: 8px;">
                    <div style="font-weight: 700; font-size: 14px; color: #004ac6; display: flex; align-items: center; gap: 6px;">
                        <span class="ms-badge-pulse"></span> MiniShop Navigator
                    </div>
                    <a href="index.html" style="font-size: 11px; color: #505f76; text-decoration: none; font-weight: 600;">Xem tất cả</a>
                </div>

                <div class="ms-sec-title">🛒 Khách Hàng (Storefront)</div>
                <a href="product-listing.html" class="ms-menu-item">🛍️ Danh Sách Sản Phẩm</a>
                <a href="product-detail.html" class="ms-menu-item">🔍 Chi Tiết Sản Phẩm</a>
                <a href="cart.html" class="ms-menu-item">🛒 Giỏ Hàng</a>
                <a href="checkout.html" class="ms-menu-item">💳 Thanh Toán (Checkout)</a>
                <a href="my-orders.html" class="ms-menu-item">📦 Lịch Sử Đơn Hàng</a>
                <a href="product-review.html" class="ms-menu-item">⭐ Đánh Giá Sản Phẩm</a>
                <a href="account.html" class="ms-menu-item">👤 Thông Tin Tài Khoản</a>
                <a href="login.html" class="ms-menu-item">🔐 Đăng Nhập / Đăng Ký</a>

                <div class="ms-sec-title">🏪 Kênh Người Bán (Seller)</div>
                <a href="my-products.html" class="ms-menu-item">📦 Sản Phẩm Của Tôi</a>
                <a href="seller-product-edit.html" class="ms-menu-item">➕ Đăng / Sửa Sản Phẩm</a>
                <a href="inventory-management.html" class="ms-menu-item">📊 Quản Lý Tồn Kho</a>
                <a href="orders-to-process.html" class="ms-menu-item">🚚 Xử Lý & Giao Đơn</a>

                <div class="ms-sec-title">🛡️ Quản Trị Viên (Admin)</div>
                <a href="admin-users.html" class="ms-menu-item">👥 Quản Lý Người Dùng</a>
                <a href="category-management.html" class="ms-menu-item">📁 Quản Lý Danh Mục</a>
                <a href="global-order-monitoring.html" class="ms-menu-item">🌐 Giám Sát Đơn Toàn Sàn</a>
                <a href="admin-inventory.html" class="ms-menu-item">📉 Kiểm Toán Tồn Kho</a>
                <a href="admin-payment-logs.html" class="ms-menu-item">💳 Đối Soát Thanh Toán</a>
                <a href="notification-logs.html" class="ms-menu-item">🔔 Nhật Ký Gửi Email/SMS</a>

                <div class="ms-sec-title">💻 Developer & Debug</div>
                <a href="debug-api-console.html" class="ms-menu-item">⚡ API Test Console</a>
                <a href="debug-system-status.html" class="ms-menu-item">📡 Trạng Thái 8 Services</a>
            </div>

            <button class="ms-switcher-btn" id="msSwitcherBtn">
                <span style="font-size: 16px;">🧭</span> Chuyển Trang Nhanh
            </button>
        `;

        document.body.appendChild(switcher);

        const btn = document.getElementById('msSwitcherBtn');
        const menu = document.getElementById('msSwitcherMenu');

        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            menu.classList.toggle('active');
        });

        document.addEventListener('click', (e) => {
            if (!switcher.contains(e.target)) {
                menu.classList.remove('active');
            }
        });

        // Highlight current page
        const currentPage = window.location.pathname.split('/').pop() || 'index.html';
        menu.querySelectorAll('.ms-menu-item').forEach(item => {
            if (item.getAttribute('href') === currentPage) {
                item.classList.add('active');
            }
        });
    }

    // Initialize on DOM Ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            linkifyDocument();
            injectQuickSwitcher();
        });
    } else {
        linkifyDocument();
        injectQuickSwitcher();
    }
})();
