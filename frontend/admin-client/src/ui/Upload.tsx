import { useEffect, useRef } from 'react';
import type { CSSProperties, ReactNode } from 'react';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import { CloseOutlined } from './icons';

export type UploadFileStatus = 'error' | 'done' | 'uploading' | 'removed';

export interface UploadFile<T = unknown> {
  uid: string;
  size?: number;
  name: string;
  fileName?: string;
  lastModified?: number;
  url?: string;
  status?: UploadFileStatus;
  percent?: number;
  thumbUrl?: string;
  originFileObj?: File;
  response?: T;
  error?: unknown;
  type?: string;
}

export interface UploadChangeParam<T = UploadFile> {
  file: T;
  fileList: T[];
  event?: { percent: number };
}

export interface RcFile extends File {
  uid: string;
}

export interface UploadRequestOptions<T = unknown> {
  file: RcFile;
  onProgress?: (percent: number) => void;
  onSuccess?: (body?: T) => void;
  onError?: (error?: Error) => void;
}

export interface UploadProps<T = unknown> {
  fileList?: Array<UploadFile<T>>;
  onChange?: (info: UploadChangeParam<UploadFile<T>>) => void;
  /** 返回 false 停止上传但文件仍进入列表(antd 语义,供调用方自行处理 originFileObj) */
  beforeUpload?: (
    file: RcFile,
    fileList: RcFile[],
  ) => boolean | void | File | Promise<boolean | void | File>;
  onPreview?: (file: UploadFile<T>) => void;
  /** 返回 false 取消移除 */
  onRemove?: (file: UploadFile<T>) => void | boolean;
  customRequest?: (options: UploadRequestOptions<T>) => void;
  listType?: 'text' | 'picture' | 'picture-card';
  multiple?: boolean;
  accept?: string;
  maxCount?: number;
  disabled?: boolean;
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
}

let uidSeed = 0;
const genUid = (): string => `aqua-upload-${Date.now()}-${uidSeed++}`;

const styles = stylex.create({
  wrapper: {
    display: 'block',
  },
  trigger: {
    display: 'inline-block',
    cursor: 'pointer',
  },
  input: {
    display: 'none',
  },
  // —— picture-card ——
  cardList: {
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'wrap',
    columnGap: '8px',
    rowGap: '8px',
  },
  cardItem: {
    position: 'relative',
    width: '96px',
    height: '96px',
    overflow: 'hidden',
    backgroundColor: tokens.bg,
    borderRadius: tokens.rSm,
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: tokens.border,
  },
  cardImg: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    display: 'block',
    cursor: 'pointer',
  },
  cardName: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    height: '100%',
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '4px',
    paddingRight: '4px',
    fontSize: '11px',
    lineHeight: '14px',
    color: tokens.textSub,
    textAlign: 'center',
    wordBreak: 'break-all',
    overflow: 'hidden',
  },
  cardMask: {
    position: 'absolute',
    top: '0px',
    right: '0px',
    bottom: '0px',
    left: '0px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    // 半透明遮罩(设计 token 无对应值,保留字面量)
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    color: tokens.onBrand,
    fontSize: '12px',
  },
  removeBtn: {
    position: 'absolute',
    top: '2px',
    right: '2px',
    width: '18px',
    height: '18px',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderWidth: '0px',
    borderStyle: 'none',
    borderRadius: '50%',
    // 半透明底(设计 token 无对应值,保留字面量)
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    color: tokens.onBrand,
    fontSize: '10px',
    cursor: 'pointer',
    zIndex: '1',
    ':hover': {
      backgroundColor: tokens.error,
    },
  },
  // —— text 列表 ——
  textList: {
    display: 'flex',
    flexDirection: 'column',
    columnGap: '0px',
    rowGap: '0px',
    marginTop: '8px',
  },
  textItem: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    columnGap: '8px',
    rowGap: '8px',
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '8px',
    paddingRight: '8px',
    fontSize: '14px',
    lineHeight: '20px',
    color: tokens.text,
    borderRadius: '4px',
    ':hover': {
      backgroundColor: tokens.bg,
    },
  },
  textName: {
    flexGrow: '1',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  textStatus: {
    flexShrink: '0',
    fontSize: '12px',
    // 颜色依赖 status,保留 style={{ }} 动态注入
  },
  textRemove: {
    flexShrink: '0',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '20px',
    height: '20px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderWidth: '0px',
    borderStyle: 'none',
    borderRadius: '4px',
    backgroundColor: 'transparent',
    color: tokens.textSub,
    cursor: 'pointer',
    ':hover': {
      color: tokens.error,
      backgroundColor: tokens.border,
    },
  },
});

export function Upload<T = unknown>({
  fileList,
  onChange,
  beforeUpload,
  onPreview,
  onRemove,
  customRequest,
  listType = 'text',
  multiple,
  accept,
  maxCount,
  disabled = false,
  className,
  style,
  children,
}: UploadProps<T>): ReactNode {
  const propsRef = useRef({ fileList, onChange, maxCount });
  // 渲染期禁止写 ref,改在提交后的 effect 中同步 latest props(仅事件/异步回调读取)
  useEffect(() => {
    propsRef.current = { fileList, onChange, maxCount };
  });

  const currentList = (): Array<UploadFile<T>> => propsRef.current.fileList ?? [];

  /** 应用 maxCount 裁剪并触发 onChange(antd onInternalChange 语义) */
  const commit = (file: UploadFile<T>, rawList: Array<UploadFile<T>>): Array<UploadFile<T>> => {
    const limit = propsRef.current.maxCount;
    let clone = [...rawList];
    let exceeded = false;
    if (limit === 1) {
      clone = clone.slice(-1);
    } else if (limit && clone.length > limit) {
      exceeded = true;
      clone = clone.slice(0, limit);
    }
    if (!exceeded || file.status === 'removed' || clone.some((f) => f.uid === file.uid)) {
      propsRef.current.onChange?.({ file, fileList: clone });
    }
    return clone;
  };

  const replaceIn = (list: Array<UploadFile<T>>, file: UploadFile<T>): Array<UploadFile<T>> =>
    list.map((f) => (f.uid === file.uid ? file : f));

  const startUpload = (uploadFile: UploadFile<T>, rc: RcFile): void => {
    if (!customRequest) return; // 无 customRequest/action 的调用路径现网未使用
    customRequest({
      file: rc,
      onProgress: (percent) => {
        const latest = currentList();
        const updated: UploadFile<T> = {
          ...(latest.find((f) => f.uid === uploadFile.uid) ?? uploadFile),
          status: 'uploading',
          percent,
        };
        commit(updated, replaceIn(latest, updated));
      },
      onSuccess: (body) => {
        const latest = currentList();
        const base = latest.find((f) => f.uid === uploadFile.uid) ?? uploadFile;
        const bodyUrl =
          body && typeof body === 'object' && 'url' in body && typeof (body as { url?: unknown }).url === 'string'
            ? ((body as { url: string }).url)
            : undefined;
        const updated: UploadFile<T> = {
          ...base,
          status: 'done',
          percent: 100,
          response: body,
          url: bodyUrl ?? base.url,
        };
        commit(updated, replaceIn(latest, updated));
      },
      onError: (error) => {
        const latest = currentList();
        const base = latest.find((f) => f.uid === uploadFile.uid) ?? uploadFile;
        const updated: UploadFile<T> = { ...base, status: 'error', error, percent: 100 };
        commit(updated, replaceIn(latest, updated));
      },
    });
  };

  const processFiles = async (files: File[]): Promise<void> => {
    if (disabled) return;
    let current: Array<UploadFile<T>> = [...currentList()];
    const originRcFiles = current
      .filter((f) => f.originFileObj)
      .map((f) => f.originFileObj as RcFile);

    for (const raw of files) {
      const rc = raw as RcFile;
      if (!rc.uid) Object.defineProperty(rc, 'uid', { value: genUid(), configurable: true });

      let parsed: boolean | void | File = true;
      if (beforeUpload) {
        parsed = await beforeUpload(rc, originRcFiles);
      }

      const uploadFile: UploadFile<T> = {
        uid: rc.uid,
        name: rc.name,
        size: rc.size,
        type: rc.type,
        originFileObj: rc,
      };

      if (parsed === false) {
        // antd:beforeUpload 返回 false 仍进入列表(无 status),调用方自行处理
        current = commit(uploadFile, [...current, uploadFile]);
        continue;
      }

      const target = (typeof parsed === 'object' ? parsed : rc) as RcFile;
      uploadFile.originFileObj = target;
      uploadFile.status = 'uploading';
      current = commit(uploadFile, [...current, uploadFile]);
      startUpload(uploadFile, target);
    }
  };

  const handleRemove = async (file: UploadFile<T>): Promise<void> => {
    const allowed = await onRemove?.(file);
    if (allowed === false) return;
    const removed: UploadFile<T> = { ...file, status: 'removed' };
    const next = currentList().filter((f) => f.uid !== file.uid);
    commit(removed, next);
  };

  const isCard = listType === 'picture-card';

  const renderFileItem = (file: UploadFile<T>): ReactNode => {
    const thumb = file.thumbUrl ?? file.url;
    const removing = (
      <button
        type="button"
        aria-label={`移除 ${file.name}`}
        onClick={() => {
          void handleRemove(file);
        }}
        {...stylex.props(isCard ? styles.removeBtn : styles.textRemove)}
      >
        <CloseOutlined />
      </button>
    );

    if (isCard) {
      return (
        <div key={file.uid} {...stylex.props(styles.cardItem)}>
          {thumb ? (
            <img
              src={thumb}
              alt={file.name}
              {...stylex.props(styles.cardImg)}
              onClick={() => onPreview?.(file)}
            />
          ) : (
            <div {...stylex.props(styles.cardName)}>{file.name}</div>
          )}
          {file.status === 'uploading' && (
            <div {...stylex.props(styles.cardMask)}>
              {file.percent != null ? `${Math.round(file.percent)}%` : '上传中'}
            </div>
          )}
          {file.status === 'error' && <div {...stylex.props(styles.cardMask)}>失败</div>}
          {removing}
        </div>
      );
    }

    const statusText =
      file.status === 'uploading'
        ? file.percent != null
          ? `${Math.round(file.percent)}%`
          : '上传中'
        : file.status === 'done'
          ? '完成'
          : file.status === 'error'
            ? '失败'
            : undefined;
    const statusColor =
      file.status === 'error'
        ? tokens.error
        : file.status === 'done'
          ? tokens.success
          : tokens.textSub;

    return (
      <div key={file.uid} {...stylex.props(styles.textItem)}>
        <span {...stylex.props(styles.textName)}>{file.name}</span>
        {statusText != null && (
          <span {...stylex.props(styles.textStatus)} style={{ color: statusColor }}>
            {statusText}
          </span>
        )}
        {removing}
      </div>
    );
  };

  const list = fileList ?? [];

  return (
    <div {...stylex.props(styles.wrapper)} className={className} style={style}>
      {children != null && (
        <label {...stylex.props(styles.trigger)}>
          <input
            type="file"
            accept={accept}
            multiple={multiple}
            disabled={disabled}
            {...stylex.props(styles.input)}
            onChange={(e) => {
              const files = e.target.files;
              if (files && files.length > 0) {
                void processFiles(Array.from(files));
              }
              // 允许重复选择同一文件
              e.target.value = '';
            }}
          />
          {children}
        </label>
      )}
      {list.length > 0 && (
        <div {...stylex.props(isCard ? styles.cardList : styles.textList)}>
          {list.map(renderFileItem)}
        </div>
      )}
    </div>
  );
}
