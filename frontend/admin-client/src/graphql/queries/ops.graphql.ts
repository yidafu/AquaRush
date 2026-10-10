import { gql } from '@apollo/client';

export const GET_BANNERS_QUERY = gql`
  query GetBanners($input: BannerListInput) {
    banners(input: $input) {
      content {
        id
        imageUrl
        title
        subtitle
        linkType
        linkTarget
        sortOrder
        enabled
        startAt
        endAt
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

export const GET_BANNER_QUERY = gql`
  query GetBanner($id: PrimaryId!) {
    banner(id: $id) {
      id
      imageUrl
      title
      subtitle
      linkType
      linkTarget
      sortOrder
      enabled
      startAt
      endAt
      createdAt
      updatedAt
    }
  }
`;

export const GET_FEEDBACKS_QUERY = gql`
  query GetFeedbacks($input: FeedbackListInput) {
    feedbacks(input: $input) {
      content {
        id
        userId
        feedbackType
        content
        images
        contactInfo
        status
        handleResult
        handledById
        handledByName
        handledAt
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_NOTICES_QUERY = gql`
  query GetNotices($input: NoticeListInput) {
    notices(input: $input) {
      content {
        id
        title
        content
        type
        pinned
        startAt
        endAt
        status
        publishedAt
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

export const GET_NOTICE_QUERY = gql`
  query GetNotice($id: PrimaryId!) {
    notice(id: $id) {
      id
      title
      content
      type
      pinned
      startAt
      endAt
      status
      publishedAt
      createdAt
      updatedAt
    }
  }
`;

export const GET_STORE_PROFILE_QUERY = gql`
  query GetStoreProfile {
    storeProfile {
      shopName
      logoUrl
      serviceHotline
      businessHoursStart
      businessHoursEnd
      shopIntro
      aboutUs
      updatedAt
    }
  }
`;

export const GET_DELIVERY_FEE_RULE_QUERY = gql`
  query GetDeliveryFeeRule {
    deliveryFeeRule {
      freeFloorLimit
      surchargeCents
      tiers {
        fromFloor
        toFloor
        feeCents
      }
      updatedAt
    }
  }
`;
