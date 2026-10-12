import { message } from '@/ui';

// 动态加载 xlsx 库
let XLSX: any = null;

export const getXlsx = async () => {
  if (!XLSX) {
    XLSX = await import('xlsx');
  }
  return XLSX;
};

// 下载模板文件
export const downloadTemplate = async () => {
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
