import { ScrollView, StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { MaxContentWidth, Spacing } from '@/constants/theme';

export interface LegalSection {
  /** 章节标题（如「第一条 服务的接受」） */
  heading: string;
  /** 正文段落，数组元素为一段 */
  paragraphs: string[];
  /** 列表项，逐条渲染为「• 」项 */
  bullets?: string[];
  /** 列表项之后的收尾段落 */
  paragraphsAfter?: string[];
}

interface LegalArticleProps {
  sections: LegalSection[];
}

/** 协议 / 政策正文容器：pageBackground 底，web 下内容居中限宽 800。 */
export function LegalArticle({ sections }: LegalArticleProps) {
  return (
    <ThemedView type="pageBackground" style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.article}>
          {sections.map((section, index) => (
            <View key={`${section.heading}-${index}`} style={styles.section}>
              <ThemedText type="heading" style={styles.heading}>
                {section.heading}
              </ThemedText>
              {section.paragraphs.map((paragraph, i) => (
                <ThemedText
                  key={i}
                  type="small"
                  themeColor="textSecondary"
                  style={styles.paragraph}>
                  {paragraph}
                </ThemedText>
              ))}
              {section.bullets?.map((bullet, i) => (
                <ThemedText
                  key={`b-${i}`}
                  type="small"
                  themeColor="textSecondary"
                  style={styles.bullet}>
                  {'• '}
                  {bullet}
                </ThemedText>
              ))}
              {section.paragraphsAfter?.map((paragraph, i) => (
                <ThemedText
                  key={`a-${i}`}
                  type="small"
                  themeColor="textSecondary"
                  style={styles.paragraph}>
                  {paragraph}
                </ThemedText>
              ))}
            </View>
          ))}
        </View>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scroll: {
    padding: Spacing.four,
    paddingBottom: Spacing.six,
  },
  article: {
    width: '100%',
    maxWidth: MaxContentWidth,
    alignSelf: 'center',
  },
  section: {
    marginBottom: Spacing.four,
  },
  heading: {
    marginBottom: Spacing.two,
  },
  paragraph: {
    lineHeight: 22,
    marginBottom: Spacing.two,
    textAlign: 'justify',
  },
  bullet: {
    lineHeight: 22,
    marginBottom: Spacing.one,
    paddingLeft: Spacing.two,
    textAlign: 'justify',
  },
});
