import { gql } from '@apollo/client';

export const GET_USERS_QUERY = gql`
  query GetUsers($input: UserListInput) {
    users(input: $input) {
      list {
        id
        wechatOpenId
        nickname
        phone
        avatarUrl
        status
        role
        customerGroupId
        customerGroupName
        createdAt
        updatedAt
      }
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
  }
`;

/** 用户持有的优惠券(用户详情页「优惠券」Tab,状态空=全部;F-MKT-003) */
export const USER_COUPONS_QUERY = gql`
  query GetUserCoupons($userId: PrimaryId!, $status: UserCouponStatus) {
    userCoupons(userId: $userId, status: $status) {
      id
      name
      couponType
      thresholdCents
      discountAmountCents
      discountRatePermille
      scope
      productId
      status
      expireTime
      usable
    }
  }
`;

/** 用户持有的桶水票(用户详情页「水票」Tab,状态空=全部;F-MKT-007) */
export const USER_TICKETS_QUERY = gql`
  query GetUserTickets($userId: PrimaryId!, $status: UserTicketStatus) {
    userTickets(userId: $userId, status: $status) {
      id
      ticketNo
      ticketTypeName
      specification
      status
      expireTime
      isGift
    }
  }
`;

/** 用户评价记录(用户详情页「评价记录」Tab,分页;F-ORD-007) */
export const REVIEWS_BY_USER_QUERY = gql`
  query GetReviewsByUser($userId: PrimaryId!, $page: Int, $size: Int) {
    reviewsByUser(userId: $userId, page: $page, size: $size) {
      content {
        reviewId
        orderId
        deliveryWorkerId
        deliveryWorkerName
        rating
        comment
        isAnonymous
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

/** 配送员收到的评价记录(账号详情页「评价记录」Tab,分页;deliveryWorkerId=账号 id,F-USR-020/021) */
export const REVIEWS_BY_DELIVERY_WORKER_QUERY = gql`
  query GetReviewsByDeliveryWorker($deliveryWorkerId: PrimaryId!, $page: Int, $size: Int) {
    reviewsByDeliveryWorker(deliveryWorkerId: $deliveryWorkerId, page: $page, size: $size) {
      content {
        reviewId
        orderId
        deliveryWorkerId
        deliveryWorkerName
        rating
        comment
        isAnonymous
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

/** 配送员评分分布/总评价数(账号详情页「评价记录」Tab;数据源 delivery_worker_statistics,F-USR-020/021) */
export const DELIVERY_WORKER_RATING_STATISTICS_QUERY = gql`
  query GetDeliveryWorkerRatingStatistics($deliveryWorkerId: PrimaryId!) {
    deliveryWorkerRatingStatistics(deliveryWorkerId: $deliveryWorkerId) {
      deliveryWorkerId
      workerName
      averageRating
      totalReviews
      oneStarReviews
      twoStarReviews
      threeStarReviews
      fourStarReviews
      fiveStarReviews
      ratingDistribution
      lastUpdated
    }
  }
`;

/** 用户收藏的商品(用户详情页「收藏商品」Tab,分页) */
export const USER_FAVORITES_QUERY = gql`
  query GetUserFavorites($userId: PrimaryId!, $page: Int, $size: Int) {
    favoriteProducts(userId: $userId, page: $page, size: $size) {
      list {
        id
        name
        subtitle
        priceCents
        originalPriceCents
        coverImageUrl
        stock
        salesVolume
        status
        addedAt
      }
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
  }
`;

/**
 * 账号分页列表(超管视图)。
 *
 * AGENTS.md 规则 10:后端 admins 已改为分页 + keyword/role 过滤,这里必须传分页参数,
 * 返回 `{ content, totalElements, ... }`(原先是一次性全量列表)。
 */
export const GET_ADMINS_QUERY = gql`
  query GetAdmins($keyword: String, $role: String, $page: Int, $size: Int) {
    admins(keyword: $keyword, role: $role, page: $page, size: $size) {
      content {
      id
      username
      realName
      phone
      role
      accountStatus
      lastLoginAt
      createdAt
      updatedAt
      avatarUrl
      onlineStatus
      isAvailable
      coordinates
      currentLocation
      rating
      totalOrders
      completedOrders
      averageRating
      earningCents
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

/** 账号详情(超管视图):按 id 查询,替代原先「拉全量 admins 再前端 find」 */
export const GET_ADMIN_QUERY = gql`
  query GetAdmin($id: PrimaryId!) {
    admin(id: $id) {
      id
      username
      realName
      phone
      role
      accountStatus
      lastLoginAt
      createdAt
      updatedAt
      avatarUrl
      onlineStatus
      isAvailable
      coordinates
      currentLocation
      rating
      totalOrders
      completedOrders
      averageRating
      earningCents
    }
  }
`;

/**
 * 配送员分页列表(普通管理员视图)。规则 10:后端改为分页 + keyword/role 过滤。
 */
export const GET_DELIVERY_WORKERS_QUERY = gql`
  query GetDeliveryWorkers($keyword: String, $role: String, $page: Int, $size: Int) {
    deliveryWorkers(keyword: $keyword, role: $role, page: $page, size: $size) {
      content {
        id
        userId
        wechatOpenId
        name
        phone
        avatarUrl
        onlineStatus
        role
        rating
        totalOrders
        completedOrders
        averageRating
        isAvailable
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const DELIVERY_WORKER_DETAIL_QUERY = gql`
  query GetDeliveryWorkerDetail($id: PrimaryId!) {
    deliveryWorker(id: $id) {
      id
      userId
      wechatOpenId
      name
      phone
      avatarUrl
      onlineStatus
      role
      rating
      totalOrders
      completedOrders
      averageRating
      isAvailable
      accountStatus
      coordinates
      currentLocation
      earningCents
      currentTaskCount
      createdAt
      updatedAt
    }
  }
`;

// 配送员订单分页查询(admin `orders` 按 deliveryWorkerId+status 过滤)
export const DELIVERY_WORKER_ORDERS_QUERY = gql`
  query GetDeliveryWorkerOrders(
    $deliveryWorkerId: PrimaryId!
    $status: OrderStatus
    $page: Int
    $size: Int
  ) {
    orders(
      input: {
        deliveryWorkerId: $deliveryWorkerId
        status: $status
        page: $page
        size: $size
      }
    ) {
      content {
        id
        orderNo
        status
        amountCents
        createdAt
        updatedAt
        completedAt
        paymentMethod
        paymentTime
        deliveryPhotos
        items {
          product {
            id
            name
            priceCents
            coverImageUrl
          }
          quantity
          unitPriceCents
          totalPriceCents
        }
        user {
          id
          nickname
          phone
          avatarUrl
        }
        address {
          id
          receiverName
          phone
          province
          city
          district
          detailAddress
        }
        deliveryWorker {
          id
          name
          phone
          avatarUrl
        }
      }
      totalElements
      totalPages
      number
      size
    }
  }
`;

/**
 * 用户详情页(UserDetailPage)单页查询:
 * 一次请求取回「用户基本信息 + 收货地址 + 分页订单 + 已完成订单数(全量口径,别名)」,
 * 满足 AGENTS.md 规则 7 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 * Tab 区块(优惠券/水票/反馈/评价/收藏)由 Tabs 懒挂载,激活时才各自请求,不在此合并。
 */
export const USER_DETAIL_PAGE_DATA_QUERY = gql`
  query UserDetailPageData($id: PrimaryId!, $page: Int!, $size: Int!) {
    user(id: $id) {
      id
      wechatOpenId
      nickname
      phone
      avatarUrl
      status
      role
      email
      totalSpentCents
      customerGroupId
      customerGroupName
      createdAt
      updatedAt
      __typename
    }
    userAddresses(userId: $id) {
      id
      province
      provinceCode
      city
      cityCode
      district
      districtCode
      receiverName
      phone
      detailAddress
      longitude
      latitude
      isDefault
    }
    orders(input: { userId: $id, page: $page, size: $size }) {
      content {
        id
        orderNo
        status
        amountCents
        createdAt
        updatedAt
        completedAt
        paymentMethod
        paymentTime
        deliveryPhotos
        items {
          product {
            id
            name
            priceCents
            coverImageUrl
          }
          quantity
          unitPriceCents
          totalPriceCents
        }
        user {
          id
          nickname
          phone
          avatarUrl
        }
        address {
          id
          receiverName
          phone
          province
          city
          district
          detailAddress
        }
        deliveryWorker {
          id
          name
          phone
          avatarUrl
        }
      }
      totalElements
      totalPages
      number
      size
    }
    completedOrders: orders(input: { userId: $id, page: 0, size: 1, status: COMPLETED }) {
      totalElements
    }
  }
`;

/** UserDetailPageData 操作名(供 refetchQueries 引用) */
export const USER_DETAIL_PAGE_DATA = 'UserDetailPageData';

/**
 * 客户列表页(CustomerUsersPage)单页查询:
 * 一次请求取回「用户分页 + 客户分组(筛选器/设置分组弹窗)」,满足
 * AGENTS.md 规则 7 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 */
export const CUSTOMER_USERS_PAGE_DATA_QUERY = gql`
  query CustomerUsersPageData($input: UserListInput) {
    users(input: $input) {
      list {
        id
        wechatOpenId
        nickname
        phone
        avatarUrl
        status
        role
        customerGroupId
        customerGroupName
        createdAt
        updatedAt
      }
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
    customerGroups(input: { page: 0, size: 200 }) {
      content {
        id
        name
        description
        memberCount
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

/** CustomerUsersPageData 操作名(供 refetchQueries 引用) */
export const CUSTOMER_USERS_PAGE_DATA = 'CustomerUsersPageData';
