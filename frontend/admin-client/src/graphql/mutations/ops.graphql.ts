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
