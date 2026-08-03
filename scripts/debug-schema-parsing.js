const fs = require('fs');

const content = fs.readFileSync('shared-config/graphql/shared-types.graphqls', 'utf8');

// Debug: Find input type definitions
const inputTypeRegex = /input\s+(\w+)\s*\{([^}]+)\}/gs;
let match;
let count = 0;

console.log('=== Parsing input types ===');

while ((match = inputTypeRegex.exec(content)) !== null) {
  count++;
  const typeName = match[1];
  const body = match[2];

  console.log(`\n${count}. Found input type: ${typeName}`);
  console.log('Body:', body.substring(0, 200) + '...');

  // Check for directives
  const hasDirectives = body.includes('@');
  console.log(`Has directives: ${hasDirectives}`);

  if (hasDirectives) {
    const lines = body.split('\n').map(line => line.trim()).filter(line => line && !line.startsWith('#'));
    console.log(`Total lines: ${lines.length}`);

    lines.forEach((line, idx) => {
      if (line.includes('@')) {
        console.log(`  Line ${idx + 1}: ${line}`);
      }
    });
  }
}

console.log(`\nTotal input types found: ${count}`);