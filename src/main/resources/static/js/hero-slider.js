/**
 * Kaveri Restaurant Group - Premium Hero Carousel Engine
 * Handles automatic crossfade rotation, desktop hover pause, touch swipe, keyboard controls, and accessibility
 */

(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', () => {
    initHeroSlider();
  });

  function initHeroSlider() {
    const sliderSection = document.getElementById('heroSliderSection');
    const slides = document.querySelectorAll('.hero-slide');
    const dots = document.querySelectorAll('.hero-dot');
    const prevBtn = document.getElementById('heroPrevBtn');
    const nextBtn = document.getElementById('heroNextBtn');

    if (!sliderSection || slides.length === 0) return;

    let currentIndex = 0;
    const totalSlides = slides.length;
    const intervalTime = 5000; // 5 seconds per slide
    let slideTimer = null;
    let isPaused = false;

    // Check for reduced motion preference
    const mediaQueryReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const prefersReducedMotion = mediaQueryReducedMotion && mediaQueryReducedMotion.matches;

    /**
     * Go to a specific slide by index
     */
    function goToSlide(index) {
      if (index === currentIndex) return;

      // Handle wrapping
      if (index < 0) {
        currentIndex = totalSlides - 1;
      } else if (index >= totalSlides) {
        currentIndex = 0;
      } else {
        currentIndex = index;
      }

      // Update slides
      slides.forEach((slide, idx) => {
        const isActive = idx === currentIndex;
        slide.classList.toggle('active', isActive);
        slide.setAttribute('aria-hidden', !isActive);
      });

      // Update dots
      dots.forEach((dot, idx) => {
        const isActive = idx === currentIndex;
        dot.classList.toggle('active', isActive);
        dot.setAttribute('aria-selected', isActive);
        dot.setAttribute('tabindex', isActive ? '0' : '-1');
      });
    }

    function nextSlide() {
      goToSlide(currentIndex + 1);
    }

    function prevSlide() {
      goToSlide(currentIndex - 1);
    }

    /**
     * Timer Management
     */
    function startTimer() {
      if (prefersReducedMotion) return; // Do not auto-rotate if reduced motion is requested
      stopTimer();
      slideTimer = setInterval(() => {
        if (!isPaused) {
          nextSlide();
        }
      }, intervalTime);
    }

    function stopTimer() {
      if (slideTimer) {
        clearInterval(slideTimer);
        slideTimer = null;
      }
    }

    function resetTimer() {
      stopTimer();
      startTimer();
    }

    // 1. Controls: Next & Prev buttons
    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        nextSlide();
        resetTimer();
      });
    }

    if (prevBtn) {
      prevBtn.addEventListener('click', () => {
        prevSlide();
        resetTimer();
      });
    }

    // 2. Controls: Pagination Dots
    dots.forEach((dot, idx) => {
      dot.addEventListener('click', () => {
        goToSlide(idx);
        resetTimer();
      });

      // Keyboard arrow navigation on dots
      dot.addEventListener('keydown', (e) => {
        if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
          e.preventDefault();
          const nextIdx = (idx + 1) % totalSlides;
          goToSlide(nextIdx);
          dots[nextIdx].focus();
          resetTimer();
        } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
          e.preventDefault();
          const prevIdx = (idx - 1 + totalSlides) % totalSlides;
          goToSlide(prevIdx);
          dots[prevIdx].focus();
          resetTimer();
        }
      });
    });

    // 3. Desktop Hover Pause
    sliderSection.addEventListener('mouseenter', () => {
      isPaused = true;
    });

    sliderSection.addEventListener('mouseleave', () => {
      isPaused = false;
    });

    // 4. Keyboard Navigation on Slider Area
    sliderSection.addEventListener('keydown', (e) => {
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
      if (e.key === 'ArrowLeft') {
        prevSlide();
        resetTimer();
      } else if (e.key === 'ArrowRight') {
        nextSlide();
        resetTimer();
      }
    });

    // 5. Mobile Touch Swipe
    let touchStartX = 0;
    let touchStartY = 0;
    let touchEndX = 0;
    let touchEndY = 0;

    sliderSection.addEventListener('touchstart', (e) => {
      if (e.touches && e.touches.length === 1) {
        touchStartX = e.touches[0].clientX;
        touchStartY = e.touches[0].clientY;
      }
    }, { passive: true });

    sliderSection.addEventListener('touchend', (e) => {
      if (e.changedTouches && e.changedTouches.length === 1) {
        touchEndX = e.changedTouches[0].clientX;
        touchEndY = e.changedTouches[0].clientY;
        handleSwipeGesture();
      }
    }, { passive: true });

    function handleSwipeGesture() {
      const diffX = touchStartX - touchEndX;
      const diffY = touchStartY - touchEndY;

      // Ensure horizontal swipe is dominant and above threshold (40px)
      if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 40) {
        if (diffX > 0) {
          // Swipe Left -> Next
          nextSlide();
        } else {
          // Swipe Right -> Prev
          prevSlide();
        }
        resetTimer();
      }
    }

    // 6. Start Initial Rotation
    startTimer();
  }
})();
