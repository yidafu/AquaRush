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
