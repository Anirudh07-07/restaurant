// ===================================================
// FoodNest - Shopping Cart JS
// Manages cart items, quantity adjustments, item deletion,
// and order summary computations via API.
// ===================================================

document.addEventListener('DOMContentLoaded', async () => {
    // Setup clear cart button
    const clearBtn = document.getElementById('clearCartBtn');
    if (clearBtn) {
        clearBtn.addEventListener('click', async () => {
            if (!confirm('Are you sure you want to clear your cart?')) return;
            try {
                await Api.delete('/cart');
                if (typeof Toast !== 'undefined') Toast.info('Cart cleared');
                await loadCart();
            } catch (err) {
                if (typeof Toast !== 'undefined') Toast.error(err.message || 'Failed to clear cart');
            }
        });
    }

    await loadCart();
});

async function loadCart() {
    const loadingEl = document.getElementById('cartLoading');
    const emptyEl = document.getElementById('cartEmpty');
    const contentEl = document.getElementById('cartContent');
    const listContainer = document.getElementById('cartItemsList');
    const container = document.getElementById('cartContainer');

    // Remove any previous error banner
    const prevError = document.getElementById('cartErrorAlert');
    if (prevError) prevError.remove();

    if (typeof Auth !== 'undefined' && !Auth.isLoggedIn()) {
        if (loadingEl) loadingEl.style.display = 'none';
        if (emptyEl) emptyEl.style.display = 'none';
        if (contentEl) contentEl.style.display = 'none';
        renderLoginPrompt(container);
        return;
    }

    try {
        if (loadingEl) loadingEl.style.display = 'block';
        if (emptyEl) emptyEl.style.display = 'none';
        if (contentEl) contentEl.style.display = 'none';

        const res = await Api.get('/cart');
        const cart = res.data;

        if (loadingEl) loadingEl.style.display = 'none';

        if (!cart || !cart.items || cart.items.length === 0) {
            if (contentEl) contentEl.style.display = 'none';
            if (emptyEl) emptyEl.style.display = 'block';
            if (typeof CartBadge !== 'undefined') CartBadge.applyBadge(0);
            return;
        }

        // Render Cart Items
        if (emptyEl) emptyEl.style.display = 'none';
        if (contentEl) contentEl.style.display = 'block';

        const itemCountEl = document.getElementById('itemCount');
        if (itemCountEl) itemCountEl.textContent = cart.itemCount || cart.items.length;

        if (listContainer) {
            listContainer.innerHTML = cart.items.map(item => `
                <div class="cart-item-card fade-in" id="cart-item-${item.cartItemId}">
                    <img src="${item.foodImageUrl || '/images/Thekaveris.jpg'}"
                         alt="${item.foodName}" class="cart-item-img"
                         onerror="this.src='/images/Thekaveris.jpg'">
                    <div class="cart-item-info">
                        <div class="cart-item-name">${item.foodName}</div>
                        <div class="cart-item-price">₹${item.unitPrice}</div>
                        <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mt-2">
                            <div class="qty-control">
                                <button type="button" class="qty-btn" onclick="updateItemQuantity(${item.cartItemId}, ${item.quantity - 1})" aria-label="Decrease quantity">−</button>
                                <span class="qty-display">${item.quantity}</span>
                                <button type="button" class="qty-btn" onclick="updateItemQuantity(${item.cartItemId}, ${item.quantity + 1})" ${item.quantity >= 20 ? 'disabled' : ''} aria-label="Increase quantity">+</button>
                            </div>
                            <div class="cart-item-subtotal">₹${item.itemSubtotal}</div>
                        </div>
                    </div>
                    <button type="button" class="remove-btn ms-2" title="Remove item" onclick="removeCartItem(${item.cartItemId})">
                        <i class="fas fa-trash-alt me-1"></i>Remove
                    </button>
                </div>
            `).join('');
        }

        // Update Summary values in cart.html
        const subtotalEl = document.getElementById('summarySubtotal');
        const taxEl = document.getElementById('summaryTax');
        const deliveryEl = document.getElementById('summaryDelivery');
        const totalEl = document.getElementById('summaryTotal');
        const freeDeliveryMsg = document.getElementById('freeDeliveryMsg');

        if (subtotalEl) subtotalEl.textContent = `₹${cart.subtotal}`;
        if (taxEl) taxEl.textContent = `₹${cart.tax}`;
        if (deliveryEl) {
            deliveryEl.innerHTML = (Number(cart.deliveryFee) === 0)
                ? '<span class="badge bg-success">FREE</span>'
                : `₹${cart.deliveryFee}`;
        }
        if (totalEl) totalEl.textContent = `₹${cart.total}`;
        if (freeDeliveryMsg) {
            if (Number(cart.subtotal) < 500) {
                freeDeliveryMsg.style.display = 'block';
                const remaining = (500 - Number(cart.subtotal)).toFixed(2);
                freeDeliveryMsg.innerHTML = `<i class="fas fa-truck me-1 text-success"></i>Add ₹${remaining} more for FREE delivery!`;
            } else {
                freeDeliveryMsg.style.display = 'block';
                freeDeliveryMsg.innerHTML = `<i class="fas fa-check-circle me-1 text-success"></i>Eligible for FREE delivery!`;
            }
        }

        if (typeof CartBadge !== 'undefined') {
            CartBadge.applyBadge(cart.itemCount || cart.items.length);
        }

    } catch (err) {
        if (loadingEl) loadingEl.style.display = 'none';
        if (emptyEl) emptyEl.style.display = 'none';
        if (contentEl) contentEl.style.display = 'none';

        if (container) {
            const alertDiv = document.createElement('div');
            alertDiv.id = 'cartErrorAlert';
            alertDiv.className = 'alert alert-danger py-4 text-center my-4 rounded-4 shadow-sm';
            alertDiv.innerHTML = `
                <i class="fas fa-exclamation-triangle fa-2x mb-3 text-danger"></i>
                <h5 class="fw-bold mb-2">Could not load your shopping cart</h5>
                <p class="text-muted mb-3">${err.message || 'Please check your connection and try again.'}</p>
                <button type="button" class="btn btn-fn rounded-pill px-4" onclick="loadCart()">
                    <i class="fas fa-redo me-2"></i>Retry
                </button>
            `;
            container.prepend(alertDiv);
        }
    }
}

function renderLoginPrompt(container) {
    if (!container) return;
    const existingPrompt = document.getElementById('cartLoginPrompt');
    if (existingPrompt) return;

    const div = document.createElement('div');
    div.id = 'cartLoginPrompt';
    div.className = 'empty-cart py-5';
    div.innerHTML = `
        <i class="fas fa-user-lock" style="font-size:3.5rem; color:var(--gold-dark); margin-bottom:16px;"></i>
        <h4 class="fw-bold mb-2" style="color:var(--primary-dark);">Sign in to view your bag</h4>
        <p class="text-muted mb-4">Please log in to your Kaveri account to view your cart items, review order totals, and checkout.</p>
        <div class="d-flex justify-content-center gap-3 flex-wrap">
            <a href="/login.html?redirect=/cart.html" class="btn btn-fn-gold rounded-pill px-4">
                <i class="fas fa-sign-in-alt me-2"></i>Sign In
            </a>
            <a href="/menu.html" class="btn btn-outline-secondary rounded-pill px-4">
                <i class="fas fa-utensils me-2"></i>Explore Menu
            </a>
        </div>
    `;
    container.appendChild(div);
}

window.updateItemQuantity = async function(cartItemId, newQty) {
    try {
        await Api.put(`/cart/items/${cartItemId}`, { quantity: newQty });
        await loadCart();
    } catch (err) {
        if (typeof Toast !== 'undefined') Toast.error(err.message || 'Failed to update item quantity');
    }
};

window.removeCartItem = async function(cartItemId) {
    try {
        await Api.delete(`/cart/items/${cartItemId}`);
        if (typeof Toast !== 'undefined') Toast.info('Item removed from cart');
        await loadCart();
    } catch (err) {
        if (typeof Toast !== 'undefined') Toast.error(err.message || 'Failed to remove item');
    }
};
