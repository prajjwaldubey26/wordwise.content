/**
 * Renders AI draft text with clean titles/subtitles instead of raw Markdown markers.
 */
function stripInlineMarkdown(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/__(.+?)__/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/^#{1,6}\s+/, '')
    .trim();
}

function classifyBlock(raw) {
  const trimmed = raw.trim();
  if (!trimmed) return null;

  const headingMatch = trimmed.match(/^(#{1,3})\s+(.+)$/);
  if (headingMatch) {
    const level = headingMatch[1].length;
    return { type: level === 1 ? 'title' : 'subtitle', text: stripInlineMarkdown(headingMatch[2]) };
  }

  const boldOnly = trimmed.match(/^\*\*(.+)\*\*$/) || trimmed.match(/^__(.+)__$/);
  if (boldOnly) {
    return { type: 'title', text: stripInlineMarkdown(boldOnly[1]) };
  }

  // Short first-line-style titles that models wrap in asterisks mid-paragraph
  if (/^\*\*.+\*\*\s*$/.test(trimmed) || /^__.+__\s*$/.test(trimmed)) {
    return { type: 'title', text: stripInlineMarkdown(trimmed) };
  }

  return { type: 'paragraph', text: stripInlineMarkdown(trimmed) };
}

export function formatContentBlocks(content = '') {
  const chunks = String(content).replace(/\r\n/g, '\n').split(/\n{2,}/);
  const blocks = [];
  let sawTitle = false;

  chunks.forEach((chunk, index) => {
    const lines = chunk.split('\n').map((line) => line.trim()).filter(Boolean);
    if (!lines.length) return;

    // Treat a short first block / bold line as the document title
    if (!sawTitle && lines.length === 1) {
      const only = lines[0];
      const asTitle =
        classifyBlock(only)?.type === 'title' ||
        (only.length <= 120 && !/[.!?]$/.test(only) && index === 0);
      if (asTitle) {
        blocks.push({ type: 'title', text: stripInlineMarkdown(only) });
        sawTitle = true;
        return;
      }
    }

    lines.forEach((line, lineIndex) => {
      const block = classifyBlock(line);
      if (!block) return;
      if (block.type === 'title') {
        if (sawTitle) {
          blocks.push({ type: 'subtitle', text: block.text });
        } else {
          blocks.push(block);
          sawTitle = true;
        }
        return;
      }
      // Keep multi-line paragraphs glued when they weren't markdown headings
      if (block.type === 'paragraph' && lineIndex > 0 && blocks.length && blocks[blocks.length - 1].type === 'paragraph') {
        blocks[blocks.length - 1].text += ` ${block.text}`;
        return;
      }
      blocks.push(block);
    });
  });

  return blocks;
}

export default function FormattedContent({ content, className = '' }) {
  const blocks = formatContentBlocks(content);

  if (!blocks.length) {
    return <article className={`formatted-content ${className}`.trim()} />;
  }

  return (
    <article className={`formatted-content ${className}`.trim()}>
      {blocks.map((block, index) => {
        if (block.type === 'title') {
          return (
            <h2 className="draft-title" key={`t-${index}`}>
              {block.text}
            </h2>
          );
        }
        if (block.type === 'subtitle') {
          return (
            <h3 className="draft-subtitle" key={`s-${index}`}>
              {block.text}
            </h3>
          );
        }
        return (
          <p className="draft-paragraph" key={`p-${index}`}>
            {block.text}
          </p>
        );
      })}
    </article>
  );
}
