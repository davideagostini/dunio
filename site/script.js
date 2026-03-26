const header = document.querySelector(".full-bleed-header");

if (header) {
  const syncScrolledState = () => {
    header.classList.toggle("is-scrolled", window.scrollY > 12);
  };

  syncScrolledState();
  window.addEventListener("scroll", syncScrolledState, { passive: true });
}
