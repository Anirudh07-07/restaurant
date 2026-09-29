// ===================================================
// FoodNest - Shopping Cart JS
// Manages cart items, quantity adjustments, item deletion,
// and order summary computations via API.
// ===================================================

document.addEventListener('DOMContentLoaded', async () => {
    if (!Auth.isLoggedIn()) {
        Toast.warning('Please log in to view your cart.');
        setTimeout(() => window.location.href = '/login.html', 1500);
        return;
    }

    await loadCart();
});

async function loadCart() {
    const listContainer = document.getElementById('cartItemsList');
    const summaryContainer = document.getElementById('cartSummaryBox');
    if (!listContainer) return;

    try {
        const res = await Api.get('/cart');
        const cart = res.data;

        if (!cart || !cart.items || cart.items.length === 0) {
            renderEmptyCart(listContainer, summaryContainer);
            CartBadge.update();
            return;
        }

        // Render Cart Items
        listContainer.innerHTML = cart.items.map(item => `
            <div class="cart-item-card fade-in">
                <img src="${item.foodImageUrl || '/images/Thekaveris.jpg'}"
                     alt="${item.foodName}" class="cart-item-img"
                     onerror="this.src='/images/Thekaveris.jpg'">
                <div class="cart-item-info">
                    <h5 class="cart-item-name">${item.foodName}</h5>
                    <div class="text-muted small mb-2">Price: ₹${item.unitPrice}</div>
                    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2">
                        <div class="qty-control">
                            <button class="qty-btn" onclick="updateItemQuantity(${item.cartItemId}, ${item.quantity - 1})">−</button>
                            <span class="fw-bold px-2">${item.quantity}</span>
                            <button class="qty-btn" onclick="updateItemQuantity(${item.cartItemId}, ${item.quantity + 1})" ${item.quantity >= 20 ? 'disabled' : ''}>+</button>
                        </div>
                        <div class="cart-item-price">₹${item.itemSubtotal}</div>
                    </div>
                </div>
                <button class="btn btn-outline-danger btn-sm rounded-circle p-2 ms-2" title="Remove item" onclick="removeCartItem(${item.cartItemId})">
                    <i class="fas fa-trash-alt"></i>
                </button>
            </div>
        `).join('');

        // Render Order Summary
        if (summaryContainer) {
            summaryContainer.innerHTML = `
                <div class="order-summary-card shadow">
                    <h4 class="mb-4" style="font-family:'Playfair Display', serif; color: var(--fn-secondary);">Order Summary</h4>
                    <div class="summary-row">
                        <span>Items (${cart.itemCount})</span>
                        <span>₹${cart.subtotal}</span>
                    </div>
                    <div class="summary-row">
                        <span>GST / Taxes (5%)</span>
                        <span>₹${cart.tax}</span>
                    </div>
                    <div class="summary-row">
                        <span>Delivery Fee</span>
                        <span>${cart.deliveryFee == 0 ? '<span class="badge bg-success">FREE</span>' : '₹' + cart.deliveryFee}</span>
                    </div>
                    ${cart.subtotal < 500 ? '<small class="text-warning d-block mt-2 mb-2">Add ₹' + (500 - cart.subtotal) + ' more for FREE delivery!</small>' : ''}
                    <hr style="border-color: rgba(255,255,255,0.1);">
                    <div class="summary-row summary-total">
                        <span>Total Payable</span>
                        <span>₹${cart.total}</span>
                    </div>
                    <div class="d-grid gap-2 mt-4">
                        <a href="/checkout.html" class="btn btn-fn btn-lg rounded-pill">Proceed to Checkout <i class="fas fa-arrow-right ms-2"></i></a>
                        <a href="/menu.html" class="btn btn-outline-light rounded-pill">Add More Items</a>
                    </div>
                </div>
            `;
        }

        CartBadge.update();

    } catch (err) {
        listContainer.innerHTML = `
            <div class="alert alert-danger py-4 text-center">
                <i class="fas fa-exclamation-circle fa-2x mb-2"></i>
                <p>Could not load cart: ${err.message}</p>
            </div>
        `;
    }
}

function renderEmptyCart(listContainer, summaryContainer) {
    listContainer.innerHTML = `
        <div class="empty-state py-5">
            <div class="empty-state-icon"><i class="fas fa-shopping-basket"></i></div>
            <h3>Your cart is empty!</h3>
            <p class="text-muted">Looks like you haven't added anything to your cart yet.</p>
            <a href="/menu.html" class="btn btn-fn rounded-pill px-4 mt-3">
                <i class="fas fa-utensils me-2"></i>Explore Our Menu
            </a>
        </div>
    `;
    if (summaryContainer) summaryContainer.innerHTML = '';
}

window.updateItemQuantity = async function(cartItemId, newQty) {
    try {
        await Api.put(`/cart/items/${cartItemId}`, { quantity: newQty });
        await loadCart();
    } catch (err) {
        Toast.error(err.message);
    }
};

window.removeCartItem = async function(cartItemId) {
    try {
        await Api.delete(`/cart/items/${cartItemId}`);
        Toast.info('Item removed from cart');
        await loadCart();
    } catch (err) {
        Toast.error(err.message);
    }
};
