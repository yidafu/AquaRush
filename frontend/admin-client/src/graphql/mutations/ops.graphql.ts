import { gql } from '@apollo/client';

export const CREATE_BANNER_MUTATION = gql`
  mutation CreateBanner($input: CreateBannerInput!) {
    createBanner(input: $input) {
      id
      title
      sortOrder
      enabled
    }
  }
`;

export const UPDATE_BANNER_MUTATION = gql`
  mutation UpdateBanner($id: PrimaryId!, $input: UpdateBannerInput!) {
    updateBanner(id: $id, input: $input) {
      id
      title
      sortOrder
      enabled
    }
  }
`;

export const DELETE_BANNER_MUTATION = gql`
  mutation DeleteBanner($id: PrimaryId!) {
    deleteBanner(id: $id)
  }
`;

export const REORDER_BANNER_MUTATION = gql`
  mutation ReorderBanner($id: PrimaryId!, $direction: ReorderDirection!) {
    reorderBanner(id: $id, direction: $direction) {
      id
      title
      sortOrder
      enabled
    }
  }
`;

export const REVIEW_FEEDBACK_MUTATION = gql`
  mutation ReviewFeedback($id: PrimaryId!, $input: FeedbackReviewInput!) {
    reviewFeedback(id: $id, input: $input) {
      id
      status
      handleResult
      handledByName
      handledAt
    }
  }
`;

export const CREATE_NOTICE_MUTATION = gql`
  mutation CreateNotice($input: CreateNoticeInput!) {
    createNotice(input: $input) {
      id
      title
      type
      pinned
      status
    }
  }
`;

export const UPDATE_NOTICE_MUTATION = gql`
  mutation UpdateNotice($id: PrimaryId!, $input: UpdateNoticeInput!) {
    updateNotice(id: $id, input: $input) {
      id
      title
      type
      pinned
      status
    }
  }
`;

export const PUBLISH_NOTICE_MUTATION = gql`
  mutation PublishNotice($id: PrimaryId!) {
    publishNotice(id: $id) {
      id
      status
      publishedAt
      pinned
    }
  }
`;

export const STOP_NOTICE_MUTATION = gql`
  mutation StopNotice($id: PrimaryId!) {
    stopNotice(id: $id) {
      id
      status
      pinned
    }
  }
`;

export const SET_NOTICE_PINNED_MUTATION = gql`
  mutation SetNoticePinned($id: PrimaryId!, $pinned: Boolean!) {
    setNoticePinned(id: $id, pinned: $pinned) {
      id
      pinned
      status
    }
  }
`;

export const DELETE_NOTICE_MUTATION = gql`
  mutation DeleteNotice($id: PrimaryId!) {
    deleteNotice(id: $id)
  }
`;

export const SAVE_STORE_PROFILE_MUTATION = gql`
  mutation SaveStoreProfile($input: SaveStoreProfileInput!) {
    saveStoreProfile(input: $input) {
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

export const SAVE_DELIVERY_FEE_RULE_MUTATION = gql`
  mutation SaveDeliveryFeeRule($input: SaveDeliveryFeeRuleInput!) {
    saveDeliveryFeeRule(input: $input) {
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
