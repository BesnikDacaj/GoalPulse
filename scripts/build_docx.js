// Builds GoalPulse_Final_Documentation.docx from Final_Project_Documentation.md.
// Run with:  node scripts/build_docx.js
// Resolves docx from the global nvm4w install if not present locally.
const path = require('path');
const fs = require('fs');

const MD_PATH    = path.resolve(__dirname, '..', 'docs', 'Final_Project_Documentation.md');
const OUT_PATH   = path.resolve(__dirname, '..', 'docs', 'GoalPulse_Final_Documentation.docx');

// Resolve docx (use a global install if local not present).
const globalRoot = require('child_process').execSync('npm root -g').toString().trim();
const docx = require(path.join(globalRoot, 'docx'));

const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, PageOrientation, LevelFormat,
  HeadingLevel, BorderStyle, WidthType, ShadingType, TableOfContents,
  PageBreak, PageNumber, TabStopType, TabStopPosition,
} = docx;

const md = fs.readFileSync(MD_PATH, 'utf8').replace(/\r\n/g, '\n').replace(/﻿/g, '');

// -- Inline parser: **bold**, *italic* (rare), `code` --
function inlineRuns(text) {
  const runs = [];
  let i = 0;
  while (i < text.length) {
    if (text[i] === '*' && text[i + 1] === '*') {
      const end = text.indexOf('**', i + 2);
      if (end !== -1) {
        runs.push(new TextRun({ text: text.slice(i + 2, end), bold: true, font: 'Calibri' }));
        i = end + 2; continue;
      }
    }
    if (text[i] === '`') {
      const end = text.indexOf('`', i + 1);
      if (end !== -1) {
        runs.push(new TextRun({ text: text.slice(i + 1, end), font: 'Consolas' }));
        i = end + 1; continue;
      }
    }
    // Plain run until next special char
    let j = i;
    while (j < text.length && text[j] !== '*' && text[j] !== '`') j++;
    if (j > i) runs.push(new TextRun({ text: text.slice(i, j), font: 'Calibri' }));
    i = j;
    if (j === i) i++; // safety
  }
  if (runs.length === 0) runs.push(new TextRun({ text: '', font: 'Calibri' }));
  return runs;
}

// -- Table builder --
const cellBorder = { style: BorderStyle.SINGLE, size: 6, color: '888888' };
const cellBorders = { top: cellBorder, bottom: cellBorder, left: cellBorder, right: cellBorder };

function buildTable(rows) {
  const colCount = rows[0].length;
  const totalWidth = 9360; // US Letter content width
  const colWidth = Math.floor(totalWidth / colCount);
  const colWidths = new Array(colCount).fill(colWidth);
  // make sure they sum to total
  colWidths[colWidths.length - 1] += totalWidth - colWidths.reduce((a, b) => a + b, 0);

  const trs = rows.map((row, idx) => new TableRow({
    tableHeader: idx === 0,
    children: row.map((cellText, cIdx) => new TableCell({
      borders: cellBorders,
      width: { size: colWidths[cIdx], type: WidthType.DXA },
      shading: idx === 0 ? { fill: 'D9E2F3', type: ShadingType.CLEAR, color: 'auto' } : undefined,
      margins: { top: 80, bottom: 80, left: 120, right: 120 },
      children: [new Paragraph({
        children: inlineRuns(cellText),
        spacing: { before: 0, after: 0 },
      })],
    })),
  }));

  return new Table({
    width: { size: totalWidth, type: WidthType.DXA },
    columnWidths: colWidths,
    rows: trs,
  });
}

// -- Markdown → Doc children --
const children = [];

// Cover page
children.push(
  new Paragraph({ spacing: { before: 2000, after: 240 }, alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: 'GoalPulse', bold: true, size: 96, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 240 },
    children: [new TextRun({ text: 'Live European Soccer Tracking and League Management', size: 36, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 800, after: 160 },
    children: [new TextRun({ text: 'Final Project Documentation', size: 36, italics: true, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 1600, after: 80 },
    children: [new TextRun({ text: 'ISTE-330 — Database Connectivity and Access', size: 28, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 80 },
    children: [new TextRun({ text: 'RIT Croatia — Spring 2026', size: 28, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 600, after: 80 },
    children: [new TextRun({ text: 'Team', bold: true, size: 28, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 },
    children: [new TextRun({ text: 'Mateo Josipović (Coordinator)', size: 24, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 },
    children: [new TextRun({ text: 'Petar Marinović', size: 24, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 },
    children: [new TextRun({ text: 'Ana Kovačić', size: 24, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 },
    children: [new TextRun({ text: 'Luka Horvat', size: 24, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 600, after: 60 },
    children: [new TextRun({ text: 'Instructor: dr. sc. Branko Mihaljević', size: 24, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 },
    children: [new TextRun({ text: 'Submission date: 2026-05-09', size: 24, font: 'Calibri' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 },
    children: [new TextRun({ text: 'Version: 1.0 (Final)', size: 24, font: 'Calibri' })] }),
  new Paragraph({ children: [new PageBreak()] })
);

// TOC page
children.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: 'Table of Contents', bold: true, font: 'Calibri' })] }),
  new TableOfContents('Table of Contents', { hyperlink: true, headingStyleRange: '1-3' }),
  new Paragraph({ children: [new PageBreak()] })
);

// Walk markdown
const lines = md.split('\n');
let idx = 0;

function isTableSeparator(s) {
  return /^\s*\|?\s*:?-+:?\s*(\|\s*:?-+:?\s*)+\|?\s*$/.test(s);
}

function splitTableRow(line) {
  let s = line.trim();
  if (s.startsWith('|')) s = s.slice(1);
  if (s.endsWith('|')) s = s.slice(0, -1);
  return s.split('|').map(c => c.trim());
}

while (idx < lines.length) {
  const line = lines[idx];

  // skip blank
  if (line.trim() === '') { idx++; continue; }

  // Code block
  if (/^```/.test(line)) {
    idx++;
    const codeLines = [];
    while (idx < lines.length && !/^```/.test(lines[idx])) {
      codeLines.push(lines[idx]); idx++;
    }
    if (idx < lines.length) idx++; // skip closing ```
    for (const cl of codeLines) {
      children.push(new Paragraph({
        spacing: { before: 0, after: 0, line: 240 },
        shading: { fill: 'F4F4F4', type: ShadingType.CLEAR, color: 'auto' },
        children: [new TextRun({ text: cl || ' ', font: 'Consolas', size: 20 })],
      }));
    }
    children.push(new Paragraph({ spacing: { after: 100 }, children: [new TextRun('')] }));
    continue;
  }

  // Headings
  let m;
  if ((m = line.match(/^#\s+(.*)$/))) {
    children.push(new Paragraph({
      heading: HeadingLevel.HEADING_1, pageBreakBefore: false,
      spacing: { before: 360, after: 200 },
      children: [new TextRun({ text: m[1].trim(), bold: true, font: 'Calibri' })],
    }));
    idx++; continue;
  }
  if ((m = line.match(/^##\s+(.*)$/))) {
    children.push(new Paragraph({
      heading: HeadingLevel.HEADING_2,
      spacing: { before: 280, after: 160 },
      children: [new TextRun({ text: m[1].trim(), bold: true, font: 'Calibri' })],
    }));
    idx++; continue;
  }
  if ((m = line.match(/^###\s+(.*)$/))) {
    children.push(new Paragraph({
      heading: HeadingLevel.HEADING_3,
      spacing: { before: 220, after: 120 },
      children: [new TextRun({ text: m[1].trim(), bold: true, font: 'Calibri' })],
    }));
    idx++; continue;
  }
  if ((m = line.match(/^####\s+(.*)$/))) {
    children.push(new Paragraph({
      heading: HeadingLevel.HEADING_4,
      spacing: { before: 200, after: 100 },
      children: [new TextRun({ text: m[1].trim(), bold: true, font: 'Calibri' })],
    }));
    idx++; continue;
  }

  // Horizontal rule
  if (/^-{3,}\s*$/.test(line)) {
    children.push(new Paragraph({
      border: { bottom: { color: 'AAAAAA', space: 1, style: BorderStyle.SINGLE, size: 6 } },
      spacing: { before: 80, after: 80 },
      children: [new TextRun('')],
    }));
    idx++; continue;
  }

  // Table — header line + separator
  if (line.includes('|') && idx + 1 < lines.length && isTableSeparator(lines[idx + 1])) {
    const rows = [];
    rows.push(splitTableRow(line));
    idx += 2; // skip separator
    while (idx < lines.length && lines[idx].includes('|') && lines[idx].trim() !== '') {
      rows.push(splitTableRow(lines[idx]));
      idx++;
    }
    children.push(buildTable(rows));
    children.push(new Paragraph({ spacing: { after: 120 }, children: [new TextRun('')] }));
    continue;
  }

  // Bulleted list
  if (/^\s*[-*]\s+/.test(line)) {
    while (idx < lines.length && /^\s*[-*]\s+/.test(lines[idx])) {
      const text = lines[idx].replace(/^\s*[-*]\s+/, '');
      children.push(new Paragraph({
        numbering: { reference: 'gp-bullets', level: 0 },
        spacing: { after: 60 },
        children: inlineRuns(text),
      }));
      idx++;
    }
    continue;
  }

  // Numbered list
  if (/^\s*\d+\.\s+/.test(line)) {
    while (idx < lines.length && /^\s*\d+\.\s+/.test(lines[idx])) {
      const text = lines[idx].replace(/^\s*\d+\.\s+/, '');
      children.push(new Paragraph({
        numbering: { reference: 'gp-numbers', level: 0 },
        spacing: { after: 60 },
        children: inlineRuns(text),
      }));
      idx++;
    }
    continue;
  }

  // Block quote
  if (/^>\s?/.test(line)) {
    const text = line.replace(/^>\s?/, '');
    children.push(new Paragraph({
      indent: { left: 720 },
      spacing: { after: 120 },
      shading: { fill: 'EFEFEF', type: ShadingType.CLEAR, color: 'auto' },
      children: inlineRuns(text),
    }));
    idx++; continue;
  }

  // Plain paragraph
  children.push(new Paragraph({
    spacing: { after: 140, line: 300 },
    children: inlineRuns(line),
  }));
  idx++;
}

const doc = new Document({
  creator: 'GoalPulse Team',
  title: 'GoalPulse — Final Project Documentation',
  description: 'ISTE-330 Final Project, Spring 2026',
  styles: {
    default: { document: { run: { font: 'Calibri', size: 22 } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 36, bold: true, font: 'Calibri', color: '1F3864' },
        paragraph: { spacing: { before: 360, after: 200 }, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 30, bold: true, font: 'Calibri', color: '2E74B5' },
        paragraph: { spacing: { before: 280, after: 160 }, outlineLevel: 1 } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 26, bold: true, font: 'Calibri', color: '2E74B5' },
        paragraph: { spacing: { before: 220, after: 120 }, outlineLevel: 2 } },
      { id: 'Heading4', name: 'Heading 4', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 24, bold: true, italics: true, font: 'Calibri', color: '2E74B5' },
        paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 3 } },
    ],
  },
  numbering: {
    config: [
      { reference: 'gp-bullets',
        levels: [
          { level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
          { level: 1, format: LevelFormat.BULLET, text: '◦', alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 1440, hanging: 360 } } } },
        ] },
      { reference: 'gp-numbers',
        levels: [
          { level: 0, format: LevelFormat.DECIMAL, text: '%1.', alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
        ] },
    ],
  },
  sections: [{
    properties: {
      page: {
        size: { width: 12240, height: 15840 },
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
      },
    },
    headers: {
      default: new Header({ children: [new Paragraph({
        alignment: AlignmentType.RIGHT,
        children: [new TextRun({ text: 'GoalPulse — Final Project Documentation', italics: true, size: 18, color: '888888' })],
      })] }),
    },
    footers: {
      default: new Footer({ children: [new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [
          new TextRun({ text: 'Page ', size: 18, color: '888888' }),
          new TextRun({ children: [PageNumber.CURRENT], size: 18, color: '888888' }),
          new TextRun({ text: ' of ', size: 18, color: '888888' }),
          new TextRun({ children: [PageNumber.TOTAL_PAGES], size: 18, color: '888888' }),
        ],
      })] }),
    },
    children,
  }],
});

(async () => {
  const buf = await Packer.toBuffer(doc);
  fs.writeFileSync(OUT_PATH, buf);
  console.log('Wrote', OUT_PATH, '(' + buf.length + ' bytes)');
})();
