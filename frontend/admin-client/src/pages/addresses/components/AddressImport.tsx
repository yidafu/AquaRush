import React, { useState } from 'react';
import { Modal, Upload, Button, Table, Alert, Space, message } from 'antd';
import { UploadOutlined, DownloadOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload/interface';
import type { AddressInput, ExcelRowData } from './types';

interface AddressImportProps {
  visible: boolean;
  onCancel: () => void;
  onImport: (addresses: AddressInput[]) => Promise<void>;
}

// 动态加载 xlsx 库
let XLSX: any = null;

const getXlsx = async () => {
  if (!XLSX) {
    XLSX = await import('xlsx');
  }
  return XLSX;
};

// 下载模板文件
const downloadTemplate = async () => {
  try {
    const XLSXModule = await getXlsx();
    const XLSXLib = XLSXModule;

    // 创建模板数据
    const templateData = [
      {
        收货人姓名: '张三',
        手机号: '13800138000',
        省: '北京市',
        市: '北京市',
        县: '朝阳区',
        详细地址: '某某街道某某小区1号楼101室',
      },
      {
        收货人姓名: '李四',
        手机号: '13900139000',
        省: '上海市',
        市: '上海市',
        县: '浦东新区',
        详细地址: '某某路某某号2号楼202室',
      },
    ];

    // 创建工作表
    const worksheet = XLSXLib.utils.json_to_sheet(templateData);

    // 设置列宽
    worksheet['!cols'] = [
      { wch: 12 }, // 收货人姓名
      { wch: 15 }, // 手机号
      { wch: 10 }, // 省
      { wch: 10 }, // 市
      { wch: 10 }, // 县
      { wch: 30 }, // 详细地址
    ];

    // 创建工作簿
    const workbook = XLSXLib.utils.book_new();
    XLSXLib.utils.book_append_sheet(workbook, worksheet, '地址导入模板');

    // 下载文件
    XLSXLib.writeFile(workbook, '地址导入模板.xlsx');
    message.success('模板文件下载成功');
  } catch (err) {
    console.error('Download template error:', err);
    message.error('下载模板文件失败');
  }
};

export const AddressImport: React.FC<AddressImportProps> = ({
  visible,
  onCancel,
  onImport,
}) => {
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [parsedData, setParsedData] = useState<AddressInput[]>([]);
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const parseExcel = async (file: File) => {
    setLoading(true);
    try {
      const XLSXModule = await getXlsx();
      const XLSXLib = XLSXModule;

      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target?.result as ArrayBuffer);
          const workbook = XLSXLib.read(data, { type: 'array' });
          const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
          const jsonData = XLSXLib.utils.sheet_to_json<ExcelRowData>(firstSheet);

          if (jsonData.length === 0) {
            setError('Excel 文件为空');
            setLoading(false);
            return;
          }

          // 验证表头
          const headers = Object.keys(jsonData[0]);
          const requiredHeaders = ['收货人姓名', '手机号', '省', '市', '县', '详细地址'];
          const missingHeaders = requiredHeaders.filter(h => !headers.includes(h));

          if (missingHeaders.length > 0) {
            setError(`缺少必要的表头字段: ${missingHeaders.join(', ')}`);
            setLoading(false);
            return;
          }

          // 转换数据
          const addresses: AddressInput[] = jsonData.map((row) => ({
            receiverName: row['收货人姓名']?.toString() || '',
            phone: row['手机号']?.toString() || '',
            province: row['省']?.toString() || '',
            city: row['市']?.toString() || '',
            district: row['县']?.toString() || '',
            detailAddress: row['详细地址']?.toString() || '',
            isDefault: false,
          }));

          // 验证数据
          const invalidRows = addresses.filter(
            (addr) => !addr.receiverName || !addr.phone || !addr.province || !addr.city || !addr.district || !addr.detailAddress
          );

          if (invalidRows.length > 0) {
            setError(`有 ${invalidRows.length} 行数据不完整，请检查`);
            setLoading(false);
            return;
          }

          setParsedData(addresses);
          setError(null);
        } catch (err) {
          console.error('Parse error:', err);
          setError('解析 Excel 文件失败，请确保文件格式正确');
        } finally {
          setLoading(false);
        }
      };
      reader.readAsArrayBuffer(file);
    } catch (err) {
      console.error('Load xlsx error:', err);
      setError('加载 Excel 解析库失败');
      setLoading(false);
    }
  };

  const handleFileChange = (info: { fileList: UploadFile[] }) => {
    const latestFile = info.fileList[info.fileList.length - 1];
    setFileList(info.fileList);

    if (latestFile?.originFileObj) {
      setParsedData([]);
      setError(null);
      parseExcel(latestFile.originFileObj);
    }
  };

  const handleImport = async () => {
    if (parsedData.length === 0) {
      message.warning('没有可导入的数据');
      return;
    }

    setImporting(true);
    try {
      await onImport(parsedData);
      message.success('导入成功');
      handleClose();
    } catch (err) {
      message.error('导入失败');
    } finally {
      setImporting(false);
    }
  };

  const handleClose = () => {
    setFileList([]);
    setParsedData([]);
    setError(null);
    onCancel();
  };

  const previewColumns = [
    {
      title: '收货人姓名',
      dataIndex: 'receiverName',
      key: 'receiverName',
      width: 100,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 120,
    },
    {
      title: '省',
      dataIndex: 'province',
      key: 'province',
      width: 80,
    },
    {
      title: '市',
      dataIndex: 'city',
      key: 'city',
      width: 80,
    },
    {
      title: '县',
      dataIndex: 'district',
      key: 'district',
      width: 80,
    },
    {
      title: '详细地址',
      dataIndex: 'detailAddress',
      key: 'detailAddress',
      ellipsis: true,
    },
  ];

  return (
    <Modal
      title="导入地址"
      open={visible}
      onCancel={handleClose}
      width={800}
      footer={[
        <Button key="cancel" onClick={handleClose}>
          取消
        </Button>,
        <Button
          key="import"
          type="primary"
          loading={importing}
          disabled={parsedData.length === 0}
          onClick={handleImport}
        >
          导入 {parsedData.length > 0 ? `(${parsedData.length} 条)` : ''}
        </Button>,
      ]}
      destroyOnClose
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Space>
          <Upload
            accept=".xlsx,.xls"
            fileList={fileList}
            onChange={handleFileChange}
            beforeUpload={() => false}
            maxCount={1}
          >
            <Button icon={<UploadOutlined />}>选择 Excel 文件</Button>
          </Upload>
          <Button icon={<DownloadOutlined />} onClick={downloadTemplate}>
            下载模板
          </Button>
        </Space>

        {loading && (
          <Alert
            type="info"
            message="正在解析 Excel 文件..."
            showIcon
          />
        )}

        {error && (
          <Alert
            type="error"
            message={error}
            showIcon
          />
        )}

        {parsedData.length > 0 && (
          <>
            <Alert
              type="info"
              message={`共解析 ${parsedData.length} 条地址数据`}
              showIcon
            />
            <Table
              columns={previewColumns}
              dataSource={parsedData}
              rowKey={(_, index) => index?.toString() || '0'}
              size="small"
              pagination={{ pageSize: 10 }}
              scroll={{ y: 300 }}
            />
          </>
        )}

        <div style={{ color: '#666', fontSize: '12px' }}>
          <p>Excel 表头要求：收货人姓名、手机号、省、市、县、详细地址</p>
          <p>说明：导入的地址将不关联任何用户 (user_id 为 null)</p>
          <p>提示：点击「下载模板」按钮获取标准模板文件</p>
        </div>
      </Space>
    </Modal>
  );
};
