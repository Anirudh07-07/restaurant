// ===================================================
// FoodNest - Home Page JS
// Loads: categories, popular dishes, reviews with skeleton loaders
// ===================================================

document.addEventListener('DOMContentLoaded', async () => {
    await Promise.all([
        loadCategories(),
        loadPopularDishes(),
        loadReviews()
    ]);
});

const categoryIcons = {
    'Pizza': '🍕', 'Burger': '🍔', 'Indian': '🍛',
    'Chinese': '🥢', 'Desserts': '🍰', 'Beverages': '☕',
    'Pasta': '🍝', 'Salad': '🥗', 'Sandwich': '🥪', 'Sushi': '🍱'
};

async function loadCategories() {
    const grid = document.getElementById('categoriesGrid');
    if (!grid) return;

    // Show skeletons
    grid.innerHTML = `
        <div class="col-lg-2 col-md-3 col-sm-4 col-6"><div class="skeleton" style="height:140px; border-radius:16px;"></div></div>
        <div class="col-lg-2 col-md-3 col-sm-4 col-6"><div class="skeleton" style="height:140px; border-radius:16px;"></div></div>
        <div class="col-lg-2 col-md-3 col-sm-4 col-6"><div class="skeleton" style="height:140px; border-radius:16px;"></div></div>
        <div class="col-lg-2 col-md-3 col-sm-4 col-6"><div class="skeleton" style="height:140px; border-radius:16px;"></div></div>
        <div class="col-lg-2 col-md-3 col-sm-4 col-6"><div class="skeleton" style="height:140px; border-radius:16px;"></div></div>
        <div class="col-lg-2 col-md-3 col-sm-4 col-6"><div class="skeleton" style="height:140px; border-radius:16px;"></div></div>
    `;

    try {
        const res = await Api.get('/categories', false);
        const categories = res.data || [];

        if (categories.length === 0) {
            grid.innerHTML = '<div class="col-12 text-center text-muted">No categories available at the moment.</div>';
            return;
        }

        grid.innerHTML = categories.map(cat => `
            <div class="col-lg-2 col-md-3 col-sm-4 col-6 fade-in">
                <a href="/menu.html?category=${cat.id}" class="category-pill-card">
                    <div class="category-icon-circle">
                        <span>${categoryIcons[cat.name] || '🍽️'}</span>
                    </div>
                    <h4 class="category-title">${cat.name}</h4>
                    <span class="category-count">${cat.description ? cat.description.substring(0, 24) + '...' : 'Explore'}</span>
                </a>
            </div>
        `).join('');
    } catch (err) {
        grid.innerHTML = '<div class="col-12 text-center text-muted">Could not load categories.</div>';
    }
}

async function loadPopularDishes() {
    const grid = document.getElementById('popularGrid');
    if (!grid) return;

    grid.innerHTML = FoodCard.skeleton(8);

    try {
        const res = await Api.get('/menu/top-rated?limit=8', false);
        const foods = res.data || [];

        if (foods.length === 0) {
            grid.innerHTML = '<div class="col-12 text-center text-muted py-5">No dishes available.</div>';
            return;
        }

        grid.innerHTML = foods.map(food => FoodCard.render(food)).join('');
    } catch (err) {
        grid.innerHTML = '<div class="col-12 text-center text-muted py-5">Could not load dishes.</div>';
    }
}

async function loadReviews() {
    const grid = document.getElementById('reviewsGrid');
    if (!grid) return;

    try {
        const res = await Api.get('/reviews/restaurant?page=0&size=3', false);
        const reviews = res.data?.content || [];

        const items = reviews.length > 0 ? reviews : getPlaceholderReviews();

        grid.innerHTML = items.map(r => `
            <div class="col-lg-4 col-md-6 fade-in">
                <div class="card border-0 p-4 shadow-sm h-100" style="background:#ffffff; border-radius:20px;">
                    <div class="d-flex align-items-center justify-content-between mb-3">
                        <div class="rating-pill fs-6">${'★'.repeat(r.rating || 5)}</div>
                        <span class="badge bg-success-subtle text-success font-sans small px-2 py-1"><i class="fas fa-check-circle me-1"></i>Verified Diner</span>
                    </div>
                    <p class="text-muted fst-italic mb-4" style="line-height:1.7;">"${r.comment || r.text || 'Exceptional food quality and wonderful hospitality!'}"</p>
                    <div class="d-flex align-items-center gap-3 mt-auto pt-3 border-top">
                        <div class="review-avatar" style="background:var(--primary);">${(r.userName || r.name || 'U').charAt(0).toUpperCase()}</div>
                        <div>
                            <h6 class="mb-0 fw-bold">${r.userName || r.name}</h6>
                            <small class="text-muted">Bangalore, India</small>
                        </div>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (err) {
        grid.innerHTML = getPlaceholderReviews().map(r => `
            <div class="col-lg-4 col-md-6 fade-in">
                <div class="card border-0 p-4 shadow-sm h-100" style="background:#ffffff; border-radius:20px;">
                    <div class="rating-pill fs-6 mb-3">${'★'.repeat(r.rating)}</div>
                    <p class="text-muted fst-italic mb-4">"${r.text}"</p>
                    <div class="d-flex align-items-center gap-3 mt-auto pt-3 border-top">
                        <div class="review-avatar" style="background:var(--primary);">${r.name.charAt(0)}</div>
                        <div>
                            <h6 class="mb-0 fw-bold">${r.name}</h6>
                            <small class="text-muted">Bangalore, India</small>
                        </div>
                    </div>
                </div>
            </div>
        `).join('');
    }
}

function getPlaceholderReviews() {
    return [
        { name: 'Priya Sundaram', rating: 5, text: 'Absolutely authentic royal flavours! The Kaveri Special Veg Thali and Paneer Butter Masala are unmatched in Ranchi. A true family tradition.' },
        { name: 'Arjun Mehta', rating: 5, text: 'The Dal Makhani and Garlic Naan were extraordinary. Four generations of consistent quality since 1947 really shows in every bite.' },
        { name: 'Ananya Roy', rating: 5, text: 'From the fresh heritage sweets of Punjab Sweet House to the crispy Masala Dosa, Kaveri never fails to impress. Highly recommended!' }
    ];
}
