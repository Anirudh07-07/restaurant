// ===================================================
// FoodNest - Main JavaScript Utility Module
// Provides: auth, API calls, toast, cart badge
// ===================================================

const API_BASE = '/api';

// ---- Auth Utilities ----
const Auth = {
    getToken: () => localStorage.getItem('fn_token'),
    getUser: () => {
        const u = localStorage.getItem('fn_user');
        return u ? JSON.parse(u) : null;
    },
    isLoggedIn: () => !!localStorage.getItem('fn_token'),
    isAdmin: () => {
        const u = Auth.getUser();
        return u && u.role === 'ROLE_ADMIN';
    },
    save: (data) => {
        localStorage.setItem('fn_token', data.token);
        localStorage.setItem('fn_user', JSON.stringify({
            id: data.userId, name: data.name, email: data.email, role: data.role
        }));
    },
    logout: () => {
        localStorage.removeItem('fn_token');
        localStorage.removeItem('fn_user');
        window.location.href = '/login.html';
    }
};

// ---- API Utilities ----
const Api = {
    headers: (includeAuth = true) => {
        const h = { 'Content-Type': 'application/json' };
        if (includeAuth && Auth.isLoggedIn()) {
            h['Authorization'] = `Bearer ${Auth.getToken()}`;
        }
        return h;
    },

    get: async (url, auth = true) => {
        const res = await fetch(API_BASE + url, { headers: Api.headers(auth) });
        return Api.handle(res);
    },

    post: async (url, body, auth = true) => {
        const res = await fetch(API_BASE + url, {
            method: 'POST',
            headers: Api.headers(auth),
            body: JSON.stringify(body)
        });
        return Api.handle(res);
    },

    put: async (url, body, auth = true) => {
        const res = await fetch(API_BASE + url, {
            method: 'PUT',
            headers: Api.headers(auth),
            body: JSON.stringify(body)
        });
        return Api.handle(res);
    },

    patch: async (url, body = null, auth = true) => {
        const res = await fetch(API_BASE + url, {
            method: 'PATCH',
            headers: Api.headers(auth),
            body: body ? JSON.stringify(body) : null
        });
        return Api.handle(res);
    },

    delete: async (url, auth = true) => {
        const res = await fetch(API_BASE + url, {
            method: 'DELETE',
            headers: Api.headers(auth)
        });
        return Api.handle(res);
    },

    uploadFile: async (url, formData) => {
        const h = {};
        if (Auth.isLoggedIn()) h['Authorization'] = `Bearer ${Auth.getToken()}`;
        const res = await fetch(API_BASE + url, {
            method: 'POST',
            headers: h,
            body: formData
        });
        return Api.handle(res);
    },

    handle: async (res) => {
        const data = await res.json().catch(() => ({}));
        if (!res.ok) {
            const msg = data.message || 'Something went wrong';
            throw new Error(msg);
        }
        return data;
    }
};

// ---- Toast Notifications ----
const Toast = {
    container: null,
    init: () => {
        if (!Toast.container) {
            Toast.container = document.createElement('div');
            Toast.container.className = 'fn-toast-container';
            document.body.appendChild(Toast.container);
        }
    },
    show: (message, type = 'success', duration = 3500) => {
        Toast.init();
        const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
        const toast = document.createElement('div');
        toast.className = `fn-toast ${type}`;
        toast.innerHTML = `<span>${icons[type] || '📢'}</span><span>${message}</span>`;
        Toast.container.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, duration);
    },
    success: (msg) => Toast.show(msg, 'success'),
    error: (msg) => Toast.show(msg, 'error'),
    warning: (msg) => Toast.show(msg, 'warning'),
    info: (msg) => Toast.show(msg, 'info')
};

// ---- Cart Badge ----
const CartBadge = {
    update: async () => {
        if (!Auth.isLoggedIn()) return;
        try {
            const res = await Api.get('/cart');
            const count = res.data?.itemCount || 0;
            const badge = document.getElementById('cartBadge');
            if (badge) {
                badge.textContent = count;
                badge.style.display = count > 0 ? 'flex' : 'none';
            }
        } catch (_) {}
    }
};

// ---- Notification Badge ----
const NotifBadge = {
    update: async () => {
        if (!Auth.isLoggedIn()) return;
        try {
            const res = await Api.get('/notifications/unread-count');
            const count = res.data || 0;
            const badge = document.getElementById('notifBadge');
            const btn = document.getElementById('notifBtn');
            if (btn) btn.style.display = 'flex';
            if (badge) {
                badge.textContent = count;
                badge.style.display = count > 0 ? 'flex' : 'none';
            }
        } catch (_) {}
    }
};

// ---- Food Card Renderer ----
const FoodCard = {
    render: (food) => {
        const vegBadge = food.vegetarian
            ? '<span class="diet-pill veg"><i class="fas fa-circle" style="font-size:0.5rem;"></i> Veg</span>'
            : '<span class="diet-pill nonveg"><i class="fas fa-circle" style="font-size:0.5rem;"></i> Non-Veg</span>';
        const spicyBadge = food.spicy ? '<span class="diet-pill spicy"><i class="fas fa-pepper-hot"></i> Spicy</span>' : '';
        const rating = food.rating ? parseFloat(food.rating).toFixed(1) : 'New';
        const imgSrc = food.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&q=80';

        return `
        <div class="col-lg-3 col-md-6 col-sm-6 fade-in">
            <div class="food-card" onclick="window.location.href='/food.html?id=${food.id}'" role="button" tabindex="0" aria-label="${food.name}">
                <div class="food-card-img-wrap">
                    <img src="${imgSrc}" alt="${food.name}" class="food-card-img" loading="lazy"
                         onerror="this.src='https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&q=80'">
                    ${vegBadge}
                    ${spicyBadge}
                    ${!food.available ? '<div class="position-absolute top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background:rgba(18,18,23,0.65); backdrop-filter:blur(3px);"><span class="badge bg-secondary px-3 py-2 rounded-pill font-sans">Unavailable Today</span></div>' : ''}
                </div>
                <div class="food-card-body">
                    <h3 class="food-card-title">${food.name}</h3>
                    <p class="food-card-desc">${food.description || 'Artisanal dish prepared with fresh seasonal ingredients.'}</p>
                    <div class="food-card-meta">
                        <span class="rating-pill"><i class="fas fa-star"></i> ${rating}</span>
                        <span><i class="far fa-clock me-1"></i>${food.preparationTime || 20} mins</span>
                        <span class="text-muted fw-semibold">${food.categoryName || 'Chef Choice'}</span>
                    </div>
                    <div class="food-card-footer">
                        <span class="food-price">₹${food.price}</span>
                        <button class="btn-card-add" onclick="event.stopPropagation(); CartActions.addToCart(${food.id}, '${food.name.replace(/'/g, "\\'")}')"
                                ${!food.available ? 'disabled' : ''} aria-label="Add ${food.name} to cart">
                            <i class="fas fa-plus"></i> Add
                        </button>
                    </div>
                </div>
            </div>
        </div>`;
    },

    skeleton: (count = 4) => {
        let html = '';
        for (let i = 0; i < count; i++) {
            html += `
            <div class="col-lg-3 col-md-6 col-sm-6">
                <div class="food-card">
                    <div class="food-card-img-wrap skeleton"></div>
                    <div class="food-card-body">
                        <div class="skeleton mb-2" style="height: 24px; width: 70%;"></div>
                        <div class="skeleton mb-3" style="height: 14px; width: 90%;"></div>
                        <div class="skeleton mb-3" style="height: 14px; width: 60%;"></div>
                        <div class="d-flex justify-content-between align-items-center pt-2">
                            <div class="skeleton" style="height: 28px; width: 30%;"></div>
                            <div class="skeleton" style="height: 36px; width: 35%; border-radius: 9999px;"></div>
                        </div>
                    </div>
                </div>
            </div>`;
        }
        return html;
    }
};

// ---- Cart Actions ----
const CartActions = {
    addToCart: async (foodItemId, foodName) => {
        if (!Auth.isLoggedIn()) {
            Toast.warning('Please login to add items to cart');
            setTimeout(() => window.location.href = '/login.html', 1000);
            return;
        }
        try {
            await Api.post('/cart/items', { foodItemId, quantity: 1 });
            Toast.success(`${foodName} added to cart!`);
            CartBadge.update();
        } catch (err) {
            Toast.error(err.message);
        }
    }
};

// ---- Navbar State ----
const Navbar = {
    init: () => {
        const user = Auth.getUser();
        const guestNav = document.getElementById('guestNav');
        const userNav = document.getElementById('userNav');
        const userName = document.getElementById('navUserName');
        const adminItem = document.getElementById('adminNavItem');

        if (user && Auth.isLoggedIn()) {
            if (guestNav) guestNav.style.display = 'none';
            if (userNav) userNav.style.display = 'block';
            if (userName) userName.textContent = user.name.split(' ')[0];
            if (adminItem && Auth.isAdmin()) adminItem.style.display = 'block';
        } else {
            if (guestNav) guestNav.style.display = 'flex';
            if (userNav) userNav.style.display = 'none';
        }

        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) logoutBtn.addEventListener('click', (e) => { e.preventDefault(); Auth.logout(); });

        // Navbar scroll effect
        window.addEventListener('scroll', () => {
            const nav = document.getElementById('mainNav');
            if (nav) nav.classList.toggle('scrolled', window.scrollY > 50);
        });

        // Back to top
        const btt = document.getElementById('backToTop');
        if (btt) {
            window.addEventListener('scroll', () => btt.classList.toggle('show', window.scrollY > 400));
            btt.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
        }
    }
};

// ---- Page Load ----
document.addEventListener('DOMContentLoaded', () => {
    Navbar.init();
    CartBadge.update();
    NotifBadge.update();
});
