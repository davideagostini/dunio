const header = document.querySelector(".full-bleed-header");

if (header) {
  const syncScrolledState = () => {
    header.classList.toggle("is-scrolled", window.scrollY > 12);
  };

  syncScrolledState();
  window.addEventListener("scroll", syncScrolledState, { passive: true });
}

const symbolCanvas = document.querySelector(".symbol-canvas");

if (
  symbolCanvas &&
  window.matchMedia("(hover: hover) and (pointer: fine)").matches &&
  window.innerWidth > 980
) {
  const context = symbolCanvas.getContext("2d");
  const symbols = ["€", "$", "+", "−", "S", "{", "}", "/", "\\", "=", "%", "&", "?", "@", "*", "<", ">", ":", "|"];
  const grid = [];
  let animationFrameId = 0;
  let mouseX = window.innerWidth * 0.72;
  let mouseY = window.innerHeight * 0.32;
  let targetX = mouseX;
  let targetY = mouseY;
  let isPointerActive = false;

  const resizeCanvas = () => {
    const ratio = window.devicePixelRatio || 1;
    symbolCanvas.width = Math.floor(window.innerWidth * ratio);
    symbolCanvas.height = Math.floor(window.innerHeight * ratio);
    symbolCanvas.style.width = `${window.innerWidth}px`;
    symbolCanvas.style.height = `${window.innerHeight}px`;
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
    buildGrid();
  };

  const buildGrid = () => {
    grid.length = 0;

    const spacing = 18;
    const margin = 80;

    for (let y = -margin; y < window.innerHeight + margin; y += spacing) {
      for (let x = -margin; x < window.innerWidth + margin; x += spacing) {
        const symbol = symbols[Math.floor(Math.random() * symbols.length)];
        grid.push({
          x: x + (Math.random() - 0.5) * 8,
          y: y + (Math.random() - 0.5) * 8,
          size: 12 + Math.random() * 3,
          color: "96, 126, 134",
          drift: (Math.random() - 0.5) * 0.25,
          symbolOffset: Math.floor(Math.random() * symbols.length),
          cadence: 300 + Math.random() * 220,
        });
      }
    }
  };

  const draw = (time) => {
    mouseX += (targetX - mouseX) * 0.22;
    mouseY += (targetY - mouseY) * 0.22;

    context.clearRect(0, 0, window.innerWidth, window.innerHeight);

    const revealRadius = 170;
    const fadeRadius = 250;
    const idleAlpha = isPointerActive ? 0 : 0.006;

    grid.forEach((item, index) => {
      const animatedX = item.x + Math.sin(time * 0.0005 + index * 0.17) * item.drift * 6;
      const animatedY = item.y + Math.cos(time * 0.00045 + index * 0.13) * item.drift * 6;
      const cycle = Math.floor((time + index * 37) / item.cadence);
      const symbol = symbols[(item.symbolOffset + cycle) % symbols.length];
      const dx = animatedX - mouseX;
      const dy = animatedY - mouseY;
      const distance = Math.sqrt(dx * dx + dy * dy);

      let alpha = idleAlpha;
      if (distance < fadeRadius) {
        const normalized = Math.max(0, 1 - (distance - revealRadius) / (fadeRadius - revealRadius));
        alpha = Math.max(alpha, Math.min(0.08, normalized * 0.075));
      }

      if (distance < revealRadius) {
        const inner = 1 - distance / revealRadius;
        alpha = Math.max(alpha, 0.055 + inner * 0.065);
      }

      if (alpha <= 0.002) {
        return;
      }

      context.save();
      context.translate(animatedX, animatedY);
      context.fillStyle = `rgba(${item.color}, ${alpha})`;
      context.font = `600 ${item.size}px Inter, system-ui, sans-serif`;
      context.textAlign = "center";
      context.textBaseline = "middle";
      context.fillText(symbol, 0, 0);
      context.restore();
    });

    animationFrameId = window.requestAnimationFrame(draw);
  };

  const handlePointerMove = (event) => {
    targetX = event.clientX;
    targetY = event.clientY;
    isPointerActive = true;
  };

  const handlePointerLeave = () => {
    isPointerActive = false;
    targetX = window.innerWidth * 0.72;
    targetY = window.innerHeight * 0.32;
  };

  resizeCanvas();
  draw();

  window.addEventListener("resize", resizeCanvas);
  window.addEventListener("mousemove", handlePointerMove, { passive: true });
  window.addEventListener("mouseleave", handlePointerLeave);
  window.addEventListener("beforeunload", () => {
    window.cancelAnimationFrame(animationFrameId);
  });
}
