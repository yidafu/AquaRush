import { gql } from '@apollo/client';

export const GET_BUSINESS_LOGS_QUERY = gql`
  query GetBusinessLogs($input: BusinessLogListInput) {
    businessLogs(input: $input) {
      list {
        id
        correlationId
        level
        loggerName
        operation
        message
        stackTrace
        userId
        username
        createdAt
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
