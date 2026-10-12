import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type { FC, ImgHTMLAttributes, ReactElement, ReactNode } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { ArrowLeftOutlined, CloseOutlined, RightOutlined } from './icons';

export interface ImageProps extends ImgHTMLAttributes<HTMLImageElement> {
  /** 点击开灯箱预览,缺省 true(antd 同义);false = 纯展示 */
  preview?: boolean;
}

/** 组内一张图:token 为登记序号(子 Image 卸载/重挂靠它定位) */
interface PreviewGroupItem {
  token: number;
  src: string;
}

/** Image.PreviewGroup 对子 Image 暴露的登记/开灯箱接口 */
interface PreviewGroupApi {
  /** 子 Image 挂载登记;srcs 固定模式返回 null(列表以 preview.srcs 为准) */
  register: (src: string) => number | null;
  unregister: (token: number) => void;
  /** token 缺失时回退按 src 定位 */
  open: (token: number | null, src: string) => void;
}

const PreviewGroupContext = createContext<PreviewGroupApi | null>(null);

const styles = stylex.create({
  img: {
    display: 'block',
    boxSizing: 'border-box',
    maxWidth: '100%',
    // width/height/alt 等由 props 决定,保留透传
  },
  zoomable: {
    cursor: 'zoom-in',
  },
  overlay: {
    position: 'fixed',
    top: '0px',
    right: '0px',
    bottom: '0px',
    left: '0px',
    // 灯箱遮罩(设计 token 无对应值,保留字面量)
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: '1100',
  },
  content: {
    position: 'relative',
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    maxWidth: '90vw',
    maxHeight: '85vh',
  },
  previewImg: {
    display: 'block',
    maxWidth: '90vw',
    maxHeight: '85vh',
    width: 'auto',
    height: 'auto',
    objectFit: 'contain',
    boxSizing: 'border-box',
  },
  close: {
    position: 'absolute',
    top: '12px',
    right: '12px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '32px',
    height: '32px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderStyle: 'none',
    borderWidth: '0px',
    borderRadius: tokens.rSm,
    backgroundColor: 'rgba(255, 255, 255, 0.12)',
    // 暗底上的关闭图标(设计 token 无对应值,保留字面量)
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: '16px',
    cursor: 'pointer',
    ':hover': {
      backgroundColor: 'rgba(255, 255, 255, 0.24)',
      color: '#FFFFFF',
    },
  },
  // Radix Dialog.Title 无障碍要求:视觉隐藏
  titleHidden: {
    position: 'absolute',
    width: '1px',
    height: '1px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    overflow: 'hidden',
    clipPath: 'inset(50%)',
    whiteSpace: 'nowrap',
    borderStyle: 'none',
    borderWidth: '0px',
  },
  // —— Image.PreviewGroup 灯箱:翻页箭头 / 计数 ——
  navButton: {
    position: 'absolute',
    top: '50%',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '40px',
    height: '40px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderStyle: 'none',
    borderWidth: '0px',
    borderRadius: '50%',
    // 暗底上的箭头按钮(设计 token 无对应值,保留字面量)
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: '18px',
    lineHeight: '1',
    cursor: 'pointer',
    ':hover': {
      backgroundColor: 'rgba(0, 0, 0, 0.75)',
      color: '#FFFFFF',
    },
  },
  navPrev: {
    left: '16px',
    marginTop: '-20px',
  },
  navNext: {
    right: '16px',
    marginTop: '-20px',
  },
  counter: {
    position: 'absolute',
    bottom: '16px',
    left: '50%',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '-32px',
    marginRight: '0px',
    minWidth: '64px',
    textAlign: 'center',
    color: 'rgba(255, 255, 255, 0.85)',
    fontFamily: tokens.font,
    fontSize: '14px',
    lineHeight: '20px',
    // 暗底计数(设计 token 无对应值,保留字面量)
    textShadow: '0 1px 3px rgba(0, 0, 0, 0.6)',
  },
});

const ImageImpl: FC<ImageProps> = ({ preview = true, className, style, onClick, ...rest }) => {
  const [open, setOpen] = useState(false);
  const group = useContext(PreviewGroupContext);
  const tokenRef = useRef<number | null>(null);
  const src = rest.src;
  const sp = stylex.props(styles.img, preview && styles.zoomable);
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined;
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style;

  // 组内登记:挂载登记、卸载摘除、src 变更重登记(src 为 effect 依赖)
  useEffect(() => {
    if (group === null || typeof src !== 'string' || src === '') return undefined;
    const token = group.register(src);
    tokenRef.current = token;
    return () => {
      if (token !== null) group.unregister(token);
      tokenRef.current = null;
    };
  }, [group, src]);

  const handleClick: NonNullable<ImgHTMLAttributes<HTMLImageElement>['onClick']> = (e) => {
    onClick?.(e);
    if (e.defaultPrevented || !preview || src == null || src === '') return;
    if (group !== null) {
      // 组模式:交给共享灯箱(全部组内图片 + 翻页)
      group.open(tokenRef.current, src);
      return;
    }
    setOpen(true);
  };

  return (
    <>
      <img {...rest} className={mergedClassName} style={mergedStyle} onClick={handleClick} />
      {open && (
        <Dialog.Root open onOpenChange={setOpen}>
          <Dialog.Portal>
            <Dialog.Overlay {...stylex.props(styles.overlay)}>
              <Dialog.Content {...stylex.props(styles.content)} aria-describedby={undefined}>
                <Dialog.Title {...stylex.props(styles.titleHidden)}>图片预览</Dialog.Title>
                <img {...stylex.props(styles.previewImg)} src={rest.src} alt={rest.alt ?? ''} />
                <Dialog.Close asChild>
                  <button type="button" aria-label="关闭" {...stylex.props(styles.close)}>
                    <CloseOutlined />
                  </button>
                </Dialog.Close>
              </Dialog.Content>
            </Dialog.Overlay>
          </Dialog.Portal>
        </Dialog.Root>
      )}
    </>
  );
};

ImageImpl.displayName = 'Image';

// ---------------------------------------------------------------------------
// Image.PreviewGroup(antd:包住一组 <Image>,点击任一进共享灯箱,可翻页 + 计数)
// ---------------------------------------------------------------------------

export interface PreviewGroupProps {
  /** antd v6 `preview.srcs`:显式组内图片(提供时子 Image 不再登记,按 src 定位);
   *  缺省收集子 <Image> 登记顺序 */
  preview?: { srcs?: readonly string[] };
  children?: ReactNode;
}

export function PreviewGroup({ preview, children }: PreviewGroupProps): ReactElement {
  // 固定列表按挂载快照(现网调用点为静态 srcs;变更场景走子 Image 登记模式)
  const [fixedSrcs] = useState<readonly string[] | undefined>(() =>
    preview?.srcs === undefined ? undefined : [...preview.srcs],
  );
  const [items, setItems] = useState<PreviewGroupItem[]>([]);
  const itemsRef = useRef<PreviewGroupItem[]>([]);
  const seqRef = useRef(0);
  const [index, setIndex] = useState(0);
  const [open, setOpen] = useState(false);

  // api 必须引用稳定:子 Image 以它为 effect 依赖,重建会导致登记循环
  const api = useMemo<PreviewGroupApi>(
    () => ({
      register(src) {
        if (fixedSrcs !== undefined) return null;
        seqRef.current += 1;
        const token = seqRef.current;
        itemsRef.current = [...itemsRef.current, { token, src }];
        setItems(itemsRef.current);
        return token;
      },
      unregister(token) {
        if (fixedSrcs !== undefined) return;
        const next = itemsRef.current.filter((item) => item.token !== token);
        if (next.length === itemsRef.current.length) return;
        itemsRef.current = next;
        setItems(next);
      },
      open(token, src) {
        let target = -1;
        if (fixedSrcs !== undefined) {
          target = fixedSrcs.indexOf(src);
        } else if (token !== null) {
          target = itemsRef.current.findIndex((item) => item.token === token);
        } else {
          target = itemsRef.current.findIndex((item) => item.src === src);
        }
        if (target < 0) return;
        setIndex(target);
        setOpen(true);
      },
    }),
    [fixedSrcs],
  );

  const srcs = fixedSrcs ?? items.map((item) => item.src);
  const total = srcs.length;
  const currentSrc = total > 0 ? srcs[Math.min(index, total - 1)] : undefined;
  const step = (delta: number): void => {
    setIndex((prev) => (prev + delta + total) % total);
  };

  return (
    <PreviewGroupContext.Provider value={api}>
      {children}
      {open && currentSrc !== undefined && (
        <Dialog.Root open onOpenChange={setOpen}>
          <Dialog.Portal>
            <Dialog.Overlay {...stylex.props(styles.overlay)}>
              <Dialog.Content {...stylex.props(styles.content)} aria-describedby={undefined}>
                <Dialog.Title {...stylex.props(styles.titleHidden)}>图片预览</Dialog.Title>
                <img {...stylex.props(styles.previewImg)} src={currentSrc} alt="" />
                {total > 1 && (
                  <>
                    <button
                      type="button"
                      aria-label="上一张"
                      {...stylex.props(styles.navButton, styles.navPrev)}
                      onClick={() => step(-1)}
                    >
                      <ArrowLeftOutlined />
                    </button>
                    <button
                      type="button"
                      aria-label="下一张"
                      {...stylex.props(styles.navButton, styles.navNext)}
                      onClick={() => step(1)}
                    >
                      <RightOutlined />
                    </button>
                    <div {...stylex.props(styles.counter)}>
                      {Math.min(index, total - 1) + 1} / {total}
                    </div>
                  </>
                )}
                <Dialog.Close asChild>
                  <button type="button" aria-label="关闭" {...stylex.props(styles.close)}>
                    <CloseOutlined />
                  </button>
                </Dialog.Close>
              </Dialog.Content>
            </Dialog.Overlay>
          </Dialog.Portal>
        </Dialog.Root>
      )}
    </PreviewGroupContext.Provider>
  );
}

PreviewGroup.displayName = 'Image.PreviewGroup';

export const Image = Object.assign(ImageImpl, { PreviewGroup });

export default Image;
