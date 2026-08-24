/*
 * 源自 LambdaLib2 (MIT),Copyright (c) Lambda Innovation, 2013-2016。
 */
package cn.lambdalib2.util.markdown;

import cn.lambdalib2.util.markdown.MarkdownParser.Attribute;

import java.util.Map;
import java.util.Set;

public interface MarkdownRenderer {

    void onTextContent(String text, Set<Attribute> attr);

    void onNewline();

    void onTag(String name, Map<String, String> attr);

}
