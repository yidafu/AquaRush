import React from 'react';
import { Form, Input, Divider, Row, Col } from '@aquarush/admin-ui';
import { SingleImageUpload, MultiImageUpload } from '../../../components/ImageUpload';
import { SimpleRichTextEditor } from '../../../components/RichTextEditor';
import { TagsEditor } from '../../../components/JsonEditor';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  jsonArea: {
    fontFamily: 'monospace',
  },
});

const { TextArea } = Input;

/** 「商品详情」面板内容(由 ProductFormFields 包在 Collapse.Panel 中渲染) */
export const ProductDetailPanel: React.FC = () => (
  <Form.Item
    label="详情内容"
    name="detailContent"
    rules={[
      { max: 5000, message: '详情内容长度不能超过5000个字符' }
    ]}
  >
    <SimpleRichTextEditor
      placeholder="请输入商品详情内容，支持 Markdown 或 HTML 格式"
      maxLength={5000}
      showCount={true}
    />
  </Form.Item>
);

/** 「媒体资源」面板内容(由 ProductFormFields 包在 Collapse.Panel 中渲染) */
export const ProductMediaPanel: React.FC = () => (
  <>
    <Row gutter={16}>
      <Col span={24}>
        <Form.Item
          label="封面图片"
          name="coverImageUrl"
          rules={[
            { required: true, message: '请上传封面图片' },
            { max: 500, message: 'URL长度不能超过500个字符' }
          ]}
        >
          <SingleImageUpload />
        </Form.Item>
      </Col>
    </Row>

    <Row gutter={16}>
      <Col span={12}>
        <Form.Item
          label="图片画廊"
          name="imageGallery"
          help="上传商品展示图片，最多10张"
          rules={[
            { max: 2000, message: '图片URL长度不能超过2000个字符' }
          ]}
        >
          <MultiImageUpload maxCount={10} />
        </Form.Item>
      </Col>
      <Col span={12}>
        <Form.Item
          label="证书图片"
          name="certificateImages"
          help="上传产品证书图片，最多5张"
          rules={[
            { max: 2000, message: '图片URL长度不能超过2000个字符' }
          ]}
        >
          <MultiImageUpload maxCount={5} />
        </Form.Item>
      </Col>
    </Row>

    <Divider>图片URL手动输入（备用）</Divider>

    <Row gutter={16}>
      <Col span={24}>
        <Form.Item
          label="封面图片URL（手动输入）"
          name="coverImageUrl"
          rules={[
            // warningOnly:URL 不合法时仅提示、不阻断提交(门面原生支持该键)
            { type: 'url', message: '请输入有效的URL地址', warningOnly: true }
          ]}
        >
          <Input placeholder="https://example.com/image.jpg" />
        </Form.Item>
      </Col>
    </Row>

    <Row gutter={16}>
      <Col span={12}>
        <Form.Item
          label="图片画廊JSON（手动输入）"
          name="imageGallery"
          help='格式：["url1", "url2", "url3"]'
        >
          <TextArea
            rows={3}
            placeholder='["https://example.com/image1.jpg", "https://example.com/image2.jpg"]'
            {...stylex.props(styles.jsonArea)}
          />
        </Form.Item>
      </Col>
      <Col span={12}>
        <Form.Item
          label="证书图片JSON（手动输入）"
          name="certificateImages"
          help='格式：["url1", "url2"]'
        >
          <TextArea
            rows={3}
            placeholder='["https://example.com/cert1.jpg", "https://example.com/cert2.jpg"]'
            {...stylex.props(styles.jsonArea)}
          />
        </Form.Item>
      </Col>
    </Row>
  </>
);

/** 「设置」面板内容(由 ProductFormFields 包在 Collapse.Panel 中渲染) */
export const ProductSettingsPanel: React.FC = () => (
  <Form.Item
    label="商品标签"
    name="tags"
  >
    <TagsEditor />
  </Form.Item>
);
