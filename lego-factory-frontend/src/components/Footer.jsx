import React from 'react';

function Footer() {
  const currentYear = new Date().getFullYear();
  
  const footerStyle = {
    marginTop: '4rem',
    padding: '2rem 1rem',
    backgroundColor: 'var(--color-surface-tertiary, #f5f5f5)',
    borderTop: '1px solid var(--color-border-primary, #ddd)',
    color: 'var(--color-text-secondary, #666)'
  };

  const containerStyle = {
    maxWidth: '1200px',
    margin: '0 auto',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '1rem'
  };

  const brandStyle = {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.25rem'
  };

  const appTitleStyle = {
    fontSize: '1.5rem',
    fontWeight: 'bold',
    color: 'var(--color-primary, #0066cc)',
    letterSpacing: '0.05em'
  };

  const subtitleStyle = {
    fontSize: '0.875rem',
    color: 'var(--color-text-tertiary, #888)',
    fontStyle: 'italic'
  };

  const linksStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: '1.5rem',
    flexWrap: 'wrap'
  };

  const linkStyle = {
    color: 'var(--color-primary, #0066cc)',
    textDecoration: 'none',
    fontSize: '0.9rem',
    fontWeight: '500',
    transition: 'color 0.2s ease'
  };

  const copyrightStyle = {
    fontSize: '0.875rem',
    color: 'var(--color-text-secondary, #666)'
  };

  return (
    <footer style={footerStyle}>
      <div style={containerStyle}>
        <div style={brandStyle}>
          <span style={appTitleStyle}>LIFE</span>
          <span style={subtitleStyle}>LEGO Integrated Factory Execution</span>
        </div>
        <div style={linksStyle}>
          <span style={copyrightStyle}>
            © {currentYear}{' '}
            <a 
              href="https://nji.io" 
              target="_blank" 
              rel="noopener noreferrer"
              style={linkStyle}
              onMouseEnter={(e) => e.target.style.textDecoration = 'underline'}
              onMouseLeave={(e) => e.target.style.textDecoration = 'none'}
            >
              nji.io
            </a>
            {' '}- All Rights Reserved
          </span>
          <a 
            href="https://github.com/IO-Nji/LIFE" 
            target="_blank" 
            rel="noopener noreferrer"
            style={linkStyle}
            onMouseEnter={(e) => e.target.style.textDecoration = 'underline'}
            onMouseLeave={(e) => e.target.style.textDecoration = 'none'}
          >
            GitHub
          </a>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
