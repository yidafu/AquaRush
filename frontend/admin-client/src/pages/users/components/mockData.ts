import type { Admin, DeliveryWorker, User, Address } from './types';

// Mock data generators for development and testing

const generateId = () => Math.random().toString(36).substring(7);

const generateRandomDate = (daysBack = 365) => {
  const date = new Date();
  date.setDate(date.getDate() - Math.floor(Math.random() * daysBack));
  return date.toISOString();
};

const generatePhoneNumber = () => {
  const prefixes = ['138', '139', '150', '151', '152', '158', '159', '176', '177', '178', '186', '187', '188'];
  const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
  const suffix = Math.floor(Math.random() * 100000000).toString().padStart(8, '0');
  return prefix + suffix;
};

const generateWechatOpenId = () => {
  return 'wx_' + Math.random().toString(36).substring(2) + Math.random().toString(36).substring(2);
};

const generateAvatarUrl = (name?: string) => {
  const seed = name || Math.random().toString(36).substring(7);
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${seed}`;
};

// Mock addresses generator
export const generateMockAddresses = (_userId: string, count = Math.floor(Math.random() * 3) + 1): Address[] => {
  const provinces = ['北京市', '上海市', '广东省', '浙江省', '江苏省', '四川省', '湖北省', '山东省'];
  const cities = {
    '北京市': ['东城区', '西城区', '朝阳区', '海淀区', '丰台区'],
    '上海市': ['黄浦区', '徐汇区', '长宁区', '静安区', '普陀区'],
    '广东省': ['广州市', '深圳市', '珠海市', '佛山市', '东莞市'],
    '浙江省': ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市'],
    '江苏省': ['南京市', '苏州市', '无锡市', '常州市', '南通市'],
    '四川省': ['成都市', '绵阳市', '德阳市', '南充市', '宜宾市'],
    '湖北省': ['武汉市', '宜昌市', '襄阳市', '荆州市', '黄冈市'],
    '山东省': ['济南市', '青岛市', '烟台市', '潍坊市', '临沂市'],
  };

  const addresses: Address[] = [];

  for (let i = 0; i < count; i++) {
    const province = provinces[Math.floor(Math.random() * provinces.length)];
    const cityList = cities[province as keyof typeof cities] || [province + '市'];
    const city = cityList[Math.floor(Math.random() * cityList.length)];
    const district = cityList[Math.floor(Math.random() * cityList.length)];

    addresses.push({
      id: generateId(),
      receiverName: `收货人${i + 1}`,
      phone: generatePhoneNumber(),
      province,
      city,
      district,
      detailAddress: `${city}${district}某某街道某某小区${Math.floor(Math.random() * 20) + 1}号楼${Math.floor(Math.random() * 10) + 1}单元${Math.floor(Math.random() * 30) + 101}室`,
      isDefault: i === 0,
      createdAt: generateRandomDate(),
      updatedAt: generateRandomDate(30),
    });
  }

  return addresses;
};

// Mock admins generator
export const generateMockAdmins = (count = 10): Admin[] => {
  const admins: Admin[] = [];
  const roles = ['SUPER_ADMIN', 'ADMIN', 'NORMAL_ADMIN'];
  const firstNames = ['张', '李', '王', '刘', '陈', '杨', '赵', '黄', '周', '吴'];
  const lastNames = ['伟', '芳', '娜', '秀英', '敏', '静', '丽', '强', '磊', '军'];

  for (let i = 0; i < count; i++) {
    const firstName = firstNames[Math.floor(Math.random() * firstNames.length)];
    const lastName = lastNames[Math.floor(Math.random() * lastNames.length)];
    const realName = firstName + lastName;

    admins.push({
      id: generateId(),
      username: `admin_${i + 1}`,
      realName,
      phone: generatePhoneNumber(),
      role: roles[Math.floor(Math.random() * roles.length)] as any,
      lastLoginAt: Math.random() > 0.3 ? generateRandomDate(7) : undefined,
      createdAt: generateRandomDate(),
      updatedAt: generateRandomDate(30),
    });
  }

  return admins;
};

// Mock delivery workers generator
export const generateMockDeliveryWorkers = (count = 15): DeliveryWorker[] => {
  const workers: DeliveryWorker[] = [];
  const statuses = ['ONLINE', 'OFFLINE'];
  const firstNames = ['张', '李', '王', '刘', '陈', '杨', '赵', '黄', '周', '吴'];
  const lastNames = ['师傅', '师傅', '师傅', '师傅', '师傅', '师傅', '师傅', '师傅', '师傅', '师傅'];
  const locations = ['朝阳区建国路', '海淀区中关村', '西城区金融街', '东城区王府井', '丰台区科技园'];

  for (let i = 0; i < count; i++) {
    const firstName = firstNames[Math.floor(Math.random() * firstNames.length)];
    const lastName = lastNames[Math.floor(Math.random() * lastNames.length)];
    const name = firstName + lastName;
    const totalOrders = Math.floor(Math.random() * 500) + 50;
    const completedOrders = Math.floor(totalOrders * (0.7 + Math.random() * 0.25));
    const rating = 3.5 + Math.random() * 1.5;

    workers.push({
      id: generateId(),
      userId: generateId(),
      wechatOpenId: generateWechatOpenId(),
      name,
      phone: generatePhoneNumber(),
      avatarUrl: generateAvatarUrl(name),
      status: statuses[Math.floor(Math.random() * statuses.length)] as any,
      coordinates: `${39.9 + Math.random() * 0.2},${116.3 + Math.random() * 0.2}`,
      currentLocation: locations[Math.floor(Math.random() * locations.length)],
      rating,
      totalOrders,
      completedOrders,
      averageRating: rating,
      earning: Math.floor(Math.random() * 50000) + 5000,
      isAvailable: Math.random() > 0.2,
      createdAt: generateRandomDate(),
      updatedAt: generateRandomDate(7),
    });
  }

  return workers;
};

// Mock users generator
export const generateMockUsers = (count = 25): User[] => {
  const users: User[] = [];
  const statuses = ['ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'];
  const roles = ['USER', 'ADMIN', 'WORKER', 'NONE'];
  const nicknames = [
    '快乐的小鸟', '阳光少年', '梦想家', '奋斗青年', '生活家',
    '美食达人', '旅行者', '读书人', '运动健将', '音乐爱好者',
    '电影迷', '游戏玩家', '摄影爱好者', '程序员', '设计师',
    '产品经理', '创业者', '自由职业者', '学生党', '上班族',
  ];

  for (let i = 0; i < count; i++) {
    const nickname = nicknames[Math.floor(Math.random() * nicknames.length)];

    users.push({
      id: generateId(),
      wechatOpenId: generateWechatOpenId(),
      nickname: Math.random() > 0.2 ? nickname : undefined,
      phone: Math.random() > 0.3 ? generatePhoneNumber() : undefined,
      avatarUrl: Math.random() > 0.4 ? generateAvatarUrl(nickname) : undefined,
      status: statuses[Math.floor(Math.random() * statuses.length)] as any,
      role: roles[Math.floor(Math.random() * roles.length)] as any,
      createdAt: generateRandomDate(),
      updatedAt: generateRandomDate(30),
    });
  }

  return users;
};

// Default mock data sets
export const mockAdmins = generateMockAdmins();
export const mockDeliveryWorkers = generateMockDeliveryWorkers();
export const mockUsers = generateMockUsers();

// Mock addresses for each user type
export const mockAdminAddresses: { [key: string]: Address[] } = {};
mockAdmins.forEach(admin => {
  mockAdminAddresses[admin.id] = generateMockAddresses(admin.id, Math.random() > 0.7 ? 0 : Math.floor(Math.random() * 2) + 1);
});

export const mockDeliveryWorkerAddresses: { [key: string]: Address[] } = {};
mockDeliveryWorkers.forEach(worker => {
  mockDeliveryWorkerAddresses[worker.id] = generateMockAddresses(worker.id, Math.floor(Math.random() * 3) + 1);
});

export const mockUserAddresses: { [key: string]: Address[] } = {};
mockUsers.forEach(user => {
  mockUserAddresses[user.id] = generateMockAddresses(user.id, Math.floor(Math.random() * 4) + 1);
});