import React from 'react';
import { Helmet } from 'react-helmet-async';

interface PageTitleProps {
  title: string;
}

const PageTitle: React.FC<PageTitleProps> = ({ title }) => (
  <Helmet>
    <title>{title} - 奔泉管理后台</title>
  </Helmet>
);

export default PageTitle;
