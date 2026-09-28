/**
 * Kaveri Restaurant Group - Dual-Mode Digital Menu Engine
 * Single Source of Truth: window.KAVERI_MENU_DATA & window.KAVERI_MENU_CATEGORIES
 * Modes: 'classic' (Default) | 'cards' (Visual Food View)
 */

(function () {
  'use strict';

  let currentCategory = 'All';
  let searchQuery = '';
  let activeViewMode = 'classic'; // 'classic' | 'cards'
  let currentDetailDish = null;

  // Initialize once DOM is ready
  document.addEventListener('DOMContentLoaded', () => {
    initMenu();
  });

  function initMenu() {
    if (!window.KAVERI_MENU_DATA || !Array.isArray(window.KAVERI_MENU_DATA)) {
      console.error('Kaveri menu data not loaded.');
      return;
    }

    // 1. Restore saved view mode from localStorage (Default: 'classic')
    const savedView = localStorage.getItem('kaveriMenuView');
    if (savedView === 'cards' || savedView === 'classic') {
      activeViewMode = savedView;
    } else {
      activeViewMode = 'classic';
    }

    // 2. Read URL params if any
    const urlParams = new URLSearchParams(window.location.search);
    const catParam = urlParams.get('category') || urlParams.get('cat');
    const qParam = urlParams.get('q') || urlParams.get('search');
    const viewParam = urlParams.get('view');

    if (viewParam === 'cards' || viewParam === 'classic') {
      activeViewMode = viewParam;
    }

    if (catParam) {
      const match = window.KAVERI_MENU_CATEGORIES.find(
        c => c.toLowerCase() === catParam.toLowerCase()
      );
      if (match) currentCategory = match;
    }

    if (qParam) {
      searchQuery = qParam.trim();
      const searchInput = document.getElementById('menuSearchInput');
      if (searchInput) searchInput.value = searchQuery;
    }

    updateViewSwitcherUI();
    renderCategoryNav();
    renderSidebarNav();
    setupEvents();
    renderDishes();
  }

  /**
   * View Switcher UI Update
   */
  function updateViewSwitcherUI() {
    const btnClassic = document.getElementById('btnViewClassic');
    const btnCards = document.getElementById('btnViewCards');

    if (btnClassic && btnCards) {
      btnClassic.classList.toggle('active', activeViewMode === 'classic');
      btnCards.classList.toggle('active', activeViewMode === 'cards');
      btnClassic.setAttribute('aria-pressed', activeViewMode === 'classic');
      btnCards.setAttribute('aria-pressed', activeViewMode === 'cards');
    }
  }

  /**
   * Switch Active View (Classic vs Cards)
   */
  function switchView(mode) {
    if (mode !== 'classic' && mode !== 'cards') return;
    activeViewMode = mode;
    localStorage.setItem('kaveriMenuView', mode);
    updateViewSwitcherUI();
    updateUrlParams();
    renderDishes();
  }

  /**
   * Setup UI Event Listeners
   */
  function setupEvents() {
    // View Switcher buttons
    const btnClassic = document.getElementById('btnViewClassic');
    const btnCards = document.getElementById('btnViewCards');

    if (btnClassic) {
      btnClassic.addEventListener('click', () => switchView('classic'));
    }
    if (btnCards) {
      btnCards.addEventListener('click', () => switchView('cards'));
    }

    // Live Search
    const searchInput = document.getElementById('menuSearchInput');
    const clearBtn = document.getElementById('clearSearchBtn');

    if (searchInput) {
      let debounceTimer;
      searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
          searchQuery = e.target.value.trim();
          if (clearBtn) {
            clearBtn.style.display = searchQuery ? 'block' : 'none';
          }
          updateUrlParams();
          renderDishes();
        }, 180);
      });
    }

    if (clearBtn) {
      clearBtn.addEventListener('click', () => {
        if (searchInput) {
          searchInput.value = '';
          searchInput.focus();
        }
        searchQuery = '';
        clearBtn.style.display = 'none';
        updateUrlParams();
        renderDishes();
      });
    }
  }

  /**
   * Render Horizontally Scrollable Category Pills
   */
  function renderCategoryNav() {
    const container = document.getElementById('categoryTabs');
    if (!container) return;

    const categories = window.KAVERI_MENU_CATEGORIES;
    const items = window.KAVERI_MENU_DATA;

    let html = '';
    categories.forEach(cat => {
      const count = cat === 'All' ? items.length : items.filter(i => i.category === cat).length;
      const isActive = cat.toLowerCase() === currentCategory.toLowerCase();

      html += `
        <button type="button" class="cat-pill-btn ${isActive ? 'active' : ''}" data-category="${escapeHtml(cat)}">
          <span>${escapeHtml(cat)}</span>
          <span class="pill-count">${count}</span>
        </button>
      `;
    });

    container.innerHTML = html;

    // Attach click listeners
    container.querySelectorAll('.cat-pill-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const selectedCat = btn.getAttribute('data-category');
        selectCategory(selectedCat);
      });
    });
  }

  /**
   * Render Desktop Sidebar Categories
   */
  function renderSidebarNav() {
    const sidebar = document.getElementById('sidebarCategoryList');
    if (!sidebar) return;

    const categories = window.KAVERI_MENU_CATEGORIES;
    const items = window.KAVERI_MENU_DATA;

    let html = '';
    categories.forEach(cat => {
      const count = cat === 'All' ? items.length : items.filter(i => i.category === cat).length;
      const isActive = cat.toLowerCase() === currentCategory.toLowerCase();

      html += `
        <a href="javascript:void(0)" class="sidebar-cat-link ${isActive ? 'active' : ''}" data-category="${escapeHtml(cat)}">
          <span>${escapeHtml(cat)}</span>
          <span class="badge-count">${count}</span>
        </a>
      `;
    });

    sidebar.innerHTML = html;

    sidebar.querySelectorAll('.sidebar-cat-link').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const selectedCat = link.getAttribute('data-category');
        selectCategory(selectedCat);
      });
    });
  }

  /**
   * Switch Active Category
   */
  function selectCategory(category) {
    currentCategory = category;
    updateUrlParams();

    // Update Pills Active Class
    document.querySelectorAll('.cat-pill-btn').forEach(btn => {
      const isCurrent = btn.getAttribute('data-category').toLowerCase() === category.toLowerCase();
      btn.classList.toggle('active', isCurrent);
      if (isCurrent) {
        btn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
      }
    });

    // Update Sidebar Active Class
    document.querySelectorAll('.sidebar-cat-link').forEach(link => {
      const isCurrent = link.getAttribute('data-category').toLowerCase() === category.toLowerCase();
      link.classList.toggle('active', isCurrent);
    });

    renderDishes();
  }

  /**
   * Main Filter & Render Dispatcher
   */
  function renderDishes() {
    const grid = document.getElementById('menuGrid');
    const statsBar = document.getElementById('menuStatsBar');
    if (!grid) return;

    const allItems = window.KAVERI_MENU_DATA;
    let filtered = allItems;

    // 1. Filter by category
    if (currentCategory && currentCategory.toLowerCase() !== 'all') {
      filtered = filtered.filter(item => item.category.toLowerCase() === currentCategory.toLowerCase());
    }

    // 2. Filter by search query
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(item =>
        item.name.toLowerCase().includes(query) ||
        item.category.toLowerCase().includes(query)
      );
    }

    // Update Results Counter Banner
    if (statsBar) {
      let label = '';
      if (searchQuery) {
        label = `Found <strong>${filtered.length}</strong> dish${filtered.length === 1 ? '' : 'es'} for "<em>${escapeHtml(searchQuery)}</em>"`;
      } else if (currentCategory === 'All') {
        label = `Showing <strong>${filtered.length}</strong> Verified Pure Vegetarian Delicacies (18 Categories)`;
      } else {
        label = `Showing <strong>${filtered.length}</strong> item${filtered.length === 1 ? '' : 's'} in <strong>${escapeHtml(currentCategory)}</strong>`;
      }
      statsBar.innerHTML = label;
    }

    // Empty state
    if (filtered.length === 0) {
      grid.innerHTML = `
        <div class="col-12 text-center py-5">
          <div class="p-5 bg-white rounded-4 border shadow-sm mx-auto" style="max-width:540px; border-color:var(--gold-border)!important;">
            <div class="mb-3">
              <i class="fas fa-utensils text-gold fa-3x" style="opacity:0.6;"></i>
            </div>
            <h4 class="font-playfair fw-bold mb-2">No Delicacies Found</h4>
            <p class="text-muted small mb-4">
              We couldn't find any menu item matching "<strong>${escapeHtml(searchQuery)}</strong>" in ${escapeHtml(currentCategory)}.
            </p>
            <button class="btn btn-fn btn-sm rounded-pill px-4" onclick="window.resetMenuFilters()">
              <i class="fas fa-rotate-left me-1"></i> Reset Filters & Show All
            </button>
          </div>
        </div>
      `;
      return;
    }

    // Render based on active view mode
    if (activeViewMode === 'classic') {
      renderClassicView(filtered, grid);
    } else {
      renderCardsView(filtered, grid);
    }
  }

  /**
   * ==========================================
   * VIEW 1: Classic Restaurant List Menu
   * ==========================================
   */
  function renderClassicView(filtered, container) {
    // If "All" category is selected and no search query, group by category
    if (currentCategory === 'All' && !searchQuery) {
      let html = '';
      const categories = window.KAVERI_MENU_CATEGORIES.filter(c => c !== 'All');

      categories.forEach(cat => {
        const catItems = filtered.filter(i => i.category === cat);
        if (catItems.length === 0) return;

        html += `
          <div class="col-12 mb-4">
            <div class="classic-menu-section-card">
              <div class="d-flex align-items-center justify-content-between pb-3 mb-3 border-bottom" style="border-color:rgba(197,155,39,0.25)!important;">
                <h3 class="font-playfair fw-bold mb-0" style="color:var(--primary-dark); font-size:1.35rem;">
                  ${escapeHtml(cat)}
                </h3>
                <span class="category-group-badge">${catItems.length} items</span>
              </div>
              <div class="row g-2">
                ${catItems.map(dish => renderClassicMenuItem(dish)).join('')}
              </div>
            </div>
          </div>
        `;
      });

      container.innerHTML = html;
    } else {
      // Single category or search results in classic mode
      let html = `
        <div class="col-12">
          <div class="classic-menu-section-card">
            <div class="d-flex align-items-center justify-content-between pb-3 mb-3 border-bottom" style="border-color:rgba(197,155,39,0.25)!important;">
              <h3 class="font-playfair fw-bold mb-0" style="color:var(--primary-dark); font-size:1.35rem;">
                ${escapeHtml(currentCategory === 'All' ? 'Search Results' : currentCategory)}
              </h3>
              <span class="category-group-badge">${filtered.length} items</span>
            </div>
            <div class="row g-2">
              ${filtered.map(dish => renderClassicMenuItem(dish)).join('')}
            </div>
          </div>
        </div>
      `;
      container.innerHTML = html;
    }
  }

  function renderClassicMenuItem(dish) {
    const isPriceReq = dish.price === null || dish.price === undefined;
    const priceFormatted = isPriceReq ? 'Price on request' : `₹${dish.price}`;
    const priceClass = isPriceReq ? 'classic-dish-price price-req' : 'classic-dish-price';

    return `
      <div class="col-lg-6 col-12">
        <div class="classic-menu-row" onclick="window.openDishDetail('${dish.id}')" title="Click to view details for ${escapeHtml(dish.name)}">
          <div class="classic-row-left">
            <span class="veg-box-symbol" title="100% Pure Vegetarian" aria-label="Pure Vegetarian">
              <span class="veg-inner-dot"></span>
            </span>
            <span class="classic-dish-name">${escapeHtml(dish.name)}</span>
            ${dish.popular ? '<span class="badge-popular-subtle" title="Verified Popular at Kaveri">★ Popular</span>' : ''}
          </div>
          <div class="classic-row-dots"></div>
          <div class="classic-row-right">
            <span class="${priceClass}">${priceFormatted}</span>
            <button type="button" class="btn-classic-quick-add" 
                    onclick="event.stopPropagation(); window.handleMenuAdd('${dish.id}', '${dish.name.replace(/'/g, "\\'")}', ${dish.price !== null ? dish.price : 'null'})" 
                    title="Add ${escapeHtml(dish.name)}">
              <i class="fas fa-plus"></i>
            </button>
          </div>
        </div>
      </div>
    `;
  }

  /**
   * ==========================================
   * VIEW 2: Premium Visual Food Card View
   * ==========================================
   */
  function renderCardsView(filtered, container) {
    // If "All" category is selected and no search query, group by category
    if (currentCategory === 'All' && !searchQuery) {
      let html = '';
      const categories = window.KAVERI_MENU_CATEGORIES.filter(c => c !== 'All');

      categories.forEach(cat => {
        const catItems = filtered.filter(i => i.category === cat);
        if (catItems.length === 0) return;

        html += `
          <div class="col-12">
            <div class="category-group-header">
              <h3 class="category-group-title">${escapeHtml(cat)}</h3>
              <span class="category-group-badge">${catItems.length} items</span>
            </div>
          </div>
        `;

        catItems.forEach(dish => {
          html += renderFoodCardItem(dish);
        });
      });

      container.innerHTML = html;
    } else {
      let html = '';
      filtered.forEach(dish => {
        html += renderFoodCardItem(dish);
      });
      container.innerHTML = html;
    }
  }

  function renderFoodCardItem(dish) {
    const isPriceReq = dish.price === null || dish.price === undefined;
    const priceFormatted = isPriceReq ? 'Price on request' : `₹${dish.price}`;
    const priceClass = isPriceReq ? 'food-card-price-value price-req' : 'food-card-price-value';
    const imgSrc = dish.imageUrl || '/images/Thekaveris.jpg';

    return `
      <div class="col-xl-4 col-lg-6 col-md-6 col-sm-12">
        <div class="food-view-card" onclick="window.openDishDetail('${dish.id}')">
          <div class="food-view-card-img-holder">
            <img src="${imgSrc}" alt="${escapeHtml(dish.name)}" class="food-view-card-img" loading="lazy"
                 onerror="this.src='/images/Thekaveris.jpg'">
            <div class="food-card-overlay-veg">
              <span class="veg-box-symbol" style="width:14px;height:14px;"><span class="veg-inner-dot" style="width:6px;height:6px;"></span></span>
              <span>Pure Veg</span>
            </div>
            ${dish.popular ? `
              <div class="food-card-overlay-popular">
                <i class="fas fa-star" style="font-size:0.65rem;"></i>
                <span>Popular</span>
              </div>
            ` : ''}
          </div>

          <div class="food-view-card-body">
            <div>
              <div class="food-card-cat-name">${escapeHtml(dish.category)}</div>
              <h4 class="food-card-dish-name">${escapeHtml(dish.name)}</h4>
              ${dish.description ? `
                <p class="food-card-description">${escapeHtml(dish.description)}</p>
              ` : '<div style="height:8px;"></div>'}
            </div>

            <div class="food-card-bottom-bar">
              <span class="${priceClass}">${priceFormatted}</span>
              <button type="button" class="btn btn-sm btn-outline-burgundy rounded-pill px-3" 
                      onclick="event.stopPropagation(); window.handleMenuAdd('${dish.id}', '${dish.name.replace(/'/g, "\\'")}', ${dish.price !== null ? dish.price : 'null'})"
                      style="font-size:0.78rem; font-weight:700;">
                <i class="fas fa-plus me-1"></i> Add
              </button>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  /**
   * Open Food Detail Modal
   */
  window.openDishDetail = function (id) {
    const dish = window.KAVERI_MENU_DATA.find(i => i.id === id);
    if (!dish) return;

    currentDetailDish = dish;

    const modalEl = document.getElementById('foodDetailModal');
    if (!modalEl) return;

    // Populate Modal Fields
    document.getElementById('modalDishName').textContent = dish.name;
    document.getElementById('modalDishCategory').textContent = dish.category;
    document.getElementById('modalDishPrice').textContent = dish.price !== null ? `₹${dish.price}` : 'Price on request';
    
    const descEl = document.getElementById('modalDishDesc');
    if (descEl) {
      descEl.textContent = dish.description || 'Artisanal dish prepared with pure refined ingredients, cow desi ghee, and authentic Kaveri recipes since 1947.';
    }

    const popBadge = document.getElementById('modalDishPopularBadge');
    if (popBadge) {
      popBadge.style.display = dish.popular ? 'inline-flex' : 'none';
    }

    const imgEl = document.getElementById('modalDishImg');
    if (imgEl) {
      imgEl.src = dish.imageUrl || '/images/Thekaveris.jpg';
      imgEl.alt = dish.name;
    }

    const addBtn = document.getElementById('modalAddBtn');
    if (addBtn) {
      addBtn.onclick = () => {
        window.handleMenuAdd(dish.id, dish.name, dish.price);
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();
      };
    }

    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();
  };

  /**
   * Update browser URL state without page reload
   */
  function updateUrlParams() {
    const params = new URLSearchParams();
    if (currentCategory && currentCategory !== 'All') {
      params.set('category', currentCategory);
    }
    if (searchQuery) {
      params.set('q', searchQuery);
    }
    if (activeViewMode && activeViewMode !== 'classic') {
      params.set('view', activeViewMode);
    }
    const newUrl = `${window.location.pathname}${params.toString() ? '?' + params.toString() : ''}`;
    window.history.replaceState({}, '', newUrl);
  }

  function escapeHtml(text) {
    if (!text) return '';
    return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // Global reset helper
  window.resetMenuFilters = function () {
    currentCategory = 'All';
    searchQuery = '';
    const searchInput = document.getElementById('menuSearchInput');
    const clearBtn = document.getElementById('clearSearchBtn');
    if (searchInput) searchInput.value = '';
    if (clearBtn) clearBtn.style.display = 'none';
    selectCategory('All');
  };

  // Global Add to Cart handler
  window.handleMenuAdd = async function (id, name, price) {
    if (price === null) {
      if (typeof Toast !== 'undefined') {
        Toast.info(`${name} is available on request at our dining tables.`);
      } else {
        alert(`${name} is available on request.`);
      }
      return;
    }

    if (typeof Auth !== 'undefined' && !Auth.isLoggedIn()) {
      if (typeof Toast !== 'undefined') {
        Toast.warning(`Please login to add ${name} to your order bag.`);
      }
      setTimeout(() => {
        window.location.href = '/login.html';
      }, 800);
      return;
    }

    if (typeof Toast !== 'undefined') {
      Toast.success(`Added ${name} (₹${price}) to your order bag.`);
      if (typeof CartBadge !== 'undefined') {
        CartBadge.update();
      }
    }
  };

})();
