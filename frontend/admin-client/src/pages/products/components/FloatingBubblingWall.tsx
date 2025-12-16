import React, { useEffect, useMemo } from 'react';

interface Bubble {
  id: string;
  x: number;
  y: number;
  size: number;
  animationDuration: number;
  delay: number;
  opacity: number;
  color: string;
  speedX: number;
  speedY: number;
}

interface FloatingBubblingWallProps {
  bubbleCount?: number;
  theme?: 'water' | 'ocean' | 'sky';
  interactive?: boolean;
}

export const FloatingBubblingWall: React.FC<FloatingBubblingWallProps> = React.memo(({
  bubbleCount = 15,
  theme = 'water',
  interactive = false
}) => {
  const containerRef = React.useRef<HTMLDivElement>(null);

  // Theme color palettes
  const themeColors = {
    water: [
      'rgba(102, 126, 234, 0.15)',
      'rgba(118, 75, 162, 0.15)',
      'rgba(64, 158, 255, 0.15)',
      'rgba(88, 86, 214, 0.15)',
      'rgba(56, 178, 172, 0.15)',
    ],
    ocean: [
      'rgba(0, 119, 190, 0.15)',
      'rgba(0, 180, 216, 0.15)',
      'rgba(144, 224, 239, 0.15)',
      'rgba(72, 202, 228, 0.15)',
      'rgba(0, 150, 199, 0.15)',
    ],
    sky: [
      'rgba(135, 206, 235, 0.15)',
      'rgba(176, 224, 230, 0.15)',
      'rgba(175, 238, 238, 0.15)',
      'rgba(64, 224, 208, 0.15)',
      'rgba(72, 209, 204, 0.15)',
    ]
  };

  const bubbles: Bubble[] = useMemo(() => {
    const colors = themeColors[theme];
    return Array.from({ length: bubbleCount }, (_, i) => ({
      id: `bubble-${i}-${Date.now()}`,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: Math.random() * 60 + 20,
      animationDuration: Math.random() * 10 + 8,
      delay: Math.random() * 5,
      opacity: Math.random() * 0.3 + 0.1,
      color: colors[Math.floor(Math.random() * colors.length)],
      speedX: (Math.random() - 0.5) * 0.5,
      speedY: -Math.random() * 2 - 1,
    }));
  }, [bubbleCount, theme]);

  useEffect(() => {
    const style = document.createElement('style');
    style.textContent = `
      .bubbling-wall {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        overflow: hidden;
        pointer-events: ${interactive ? 'auto' : 'none'};
        z-index: 1;
      }

      .bubble {
        position: absolute;
        border-radius: 50%;
        background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.6), var(--bubble-color));
        backdrop-filter: blur(2px);
        will-change: transform, opacity;
        transition: transform 0.3s ease-out;
        animation: float-up var(--duration) infinite ease-in-out;
        animation-delay: var(--delay);
      }

      .bubble:hover {
        transform: scale(1.1);
        opacity: 0.8;
      }

      @keyframes float-up {
        0% {
          transform: translateY(100vh) translateX(0) scale(0);
          opacity: 0;
        }
        10% {
          opacity: var(--opacity);
        }
        30% {
          transform: translateY(70vh) translateX(20px) scale(1);
        }
        60% {
          transform: translateY(40vh) translateX(-15px) scale(1.1);
        }
        90% {
          opacity: var(--opacity);
        }
        100% {
          transform: translateY(-100vh) translateX(10px) scale(0.8);
          opacity: 0;
        }
      }

      @keyframes gentle-float {
        0%, 100% {
          transform: translateY(0) translateX(0) scale(1);
        }
        25% {
          transform: translateY(-10px) translateX(5px) scale(1.02);
        }
        50% {
          transform: translateY(5px) translateX(-3px) scale(0.98);
        }
        75% {
          transform: translateY(-5px) translateX(-5px) scale(1.01);
        }
      }
    `;
    document.head.appendChild(style);

    return () => {
      document.head.removeChild(style);
    };
  }, [interactive]);

  useEffect(() => {
    if (!interactive || !containerRef.current) return;

    const handleMouseMove = (e: MouseEvent) => {
      const rect = containerRef.current?.getBoundingClientRect();
      if (!rect) return;

      const x = (e.clientX - rect.left) / rect.width;
      const y = (e.clientY - rect.top) / rect.height;

      const bubbles = containerRef.current?.querySelectorAll('.bubble');
      bubbles?.forEach((bubble, index) => {
        const delay = index * 0.02;
        setTimeout(() => {
          const currentX = parseFloat((bubble as HTMLElement).style.left || '50');
          const currentY = parseFloat((bubble as HTMLElement).style.bottom || '50');
          const targetX = currentX + (x - 0.5) * 10;
          const targetY = currentY + (0.5 - y) * 10;

          (bubble as HTMLElement).style.transform = `translate(${targetX - currentX}px, ${targetY - currentY}px)`;
        }, delay * 1000);
      });
    };

    const container = containerRef.current;
    container.addEventListener('mousemove', handleMouseMove);

    return () => {
      container.removeEventListener('mousemove', handleMouseMove);
    };
  }, [interactive]);

  return (
    <div ref={containerRef} className="bubbling-wall">
      {bubbles.map((bubble) => (
        <div
          key={bubble.id}
          className="bubble"
          style={{
            left: `${bubble.x}%`,
            bottom: '-100px',
            width: `${bubble.size}px`,
            height: `${bubble.size}px`,
            '--bubble-color': bubble.color,
            '--duration': `${bubble.animationDuration}s`,
            '--delay': `${bubble.delay}s`,
            '--opacity': bubble.opacity,
          } as React.CSSProperties}
        />
      ))}
    </div>
  );
});

FloatingBubblingWall.displayName = 'FloatingBubblingWall';