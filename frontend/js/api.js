/**
 * MiniShop API Client
 * Centralized API service connecting the frontend to API Gateway (http://localhost:8080)
 */

const API_BASE_URL = 'http://localhost:8080/api/v1';

// Token Management
export const authService = {
    getToken: () => localStorage.getItem('minishop_access_token'),
    setToken: (token) => localStorage.setItem('minishop_access_token', token),
    getRefreshToken: () => localStorage.getItem('minishop_refresh_token'),
    setRefreshToken: (token) => localStorage.setItem('minishop_refresh_token', token),
    getUser: () => {
        const user = localStorage.getItem('minishop_user');
        return user ? JSON.parse(user) : null;
    },
    setUser: (user) => localStorage.setItem('minishop_user', JSON.stringify(user)),
    formatVND: (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0),
    logout: () => {
        localStorage.removeItem('minishop_access_token');
        localStorage.removeItem('minishop_refresh_token');
        localStorage.removeItem('minishop_user');
        window.location.href = 'login.html';
    }
};

// Base Fetch with Auth Interceptor
async function request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const token = authService.getToken();

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(url, { ...options, headers });
        const data = response.status !== 204 ? await response.json() : null;

        if (!response.ok) {
            if (response.status === 401) {
                authService.logout();
            }
            throw new Error(data?.message || `HTTP ${response.status}: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error(`API Error [${endpoint}]:`, error);
        throw error;
    }
}

// 1. Auth & User APIs
export const api = {
    auth: {
        register: (data) => request('/auth/register', { method: 'POST', body: JSON.stringify(data) }),
        login: (data) => request('/auth/login', { method: 'POST', body: JSON.stringify(data) }),
        refresh: (refreshToken) => request('/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) }),
        getMe: () => request('/users/me'),
        updateMe: (data) => request('/users/me', { method: 'PUT', body: JSON.stringify(data) })
    },

    // 2. Product & Category APIs
    products: {
        list: (params = '') => request(`/products${params}`),
        getById: (id) => request(`/products/${id}`),
        create: (data) => request('/products', { method: 'POST', body: JSON.stringify(data) }),
        update: (id, data) => request(`/products/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
        delete: (id) => request(`/products/${id}`, { method: 'DELETE' }),
        listCategories: () => request('/categories'),
        createCategory: (data) => request('/categories', { method: 'POST', body: JSON.stringify(data) })
    },

    // 3. Cart & Order APIs
    cart: {
        get: () => request('/cart'),
        addItem: (productId, quantity = 1) => request('/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity }) }),
        updateItem: (itemId, quantity) => request(`/cart/items/${itemId}`, { method: 'PUT', body: JSON.stringify({ quantity }) }),
        removeItem: (itemId) => request(`/cart/items/${itemId}`, { method: 'DELETE' }),
        clear: () => request('/cart', { method: 'DELETE' })
    },

    orders: {
        checkout: (data) => request('/orders/checkout', { method: 'POST', body: JSON.stringify(data) }),
        getMyOrders: (params = '') => request(`/orders${params}`),
        getById: (id) => request(`/orders/${id}`),
        cancel: (id, reason) => request(`/orders/${id}/cancel`, { method: 'POST', body: JSON.stringify({ reason }) }),
        updateStatus: (id, status) => request(`/orders/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) })
    },

    // 4. Inventory APIs
    inventory: {
        getByProduct: (productId) => request(`/inventory/${productId}`),
        importStock: (data) => request('/inventory/import', { method: 'POST', body: JSON.stringify(data) }),
        adjustStock: (productId, data) => request(`/inventory/${productId}/adjust`, { method: 'PUT', body: JSON.stringify(data) }),
        getMovements: (productId) => request(`/inventory/${productId}/movements`)
    },

    // 5. Payment APIs
    payments: {
        getStatus: (orderId) => request(`/payments/${orderId}/status`),
        getCallbackLogs: (orderId) => request(`/payments/${orderId}/callback-logs`)
    },

    // 6. Review APIs
    reviews: {
        getByProduct: (productId, params = '') => request(`/reviews/product/${productId}${params}`),
        getSummary: (productId) => request(`/reviews/product/${productId}/summary`),
        create: (data) => request('/reviews', { method: 'POST', body: JSON.stringify(data) }),
        reply: (reviewId, content) => request(`/reviews/${reviewId}/reply`, { method: 'POST', body: JSON.stringify({ content }) }),
        hide: (reviewId) => request(`/reviews/${reviewId}/hide`, { method: 'PUT' })
    },

    // 7. Notification Logs (Admin)
    notifications: {
        getLogs: (params = '') => request(`/notifications/logs${params}`),
        resend: (logId) => request(`/notifications/${logId}/resend`, { method: 'POST' })
    }
};
