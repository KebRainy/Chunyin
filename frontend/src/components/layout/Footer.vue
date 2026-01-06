<template>
  <div class="footer-container">
    <div class="footer-content">
      <div class="footer-brand">
        <div class="footer-logo art-title">醇饮</div>
        <p class="footer-tagline">探索酒饮世界的无限可能</p>
      </div>
      <div class="footer-links">
        <a href="#" class="footer-link">关于我们</a>
        <a href="#" class="footer-link">使用条款</a>
        <a href="#" class="footer-link">隐私政策</a>
        <a href="#" class="footer-link">联系我们</a>
      </div>
    </div>
    <div class="footer-bottom">
      <p>&copy; 2025 醇饮社区 | 酒精饮料信息分享平台</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Footer'
}
</script>

<style scoped>
.footer-container {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border-top: 1px solid rgba(139, 69, 19, 0.08);
  padding: 32px 24px 24px;
  margin-top: 40px;
  position: relative;
}

.footer-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, 
    transparent 0%, 
    rgba(139, 69, 19, 0.2) 20%, 
    rgba(139, 69, 19, 0.2) 80%, 
    transparent 100%);
}

.footer-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 32px;
}

.footer-brand {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-width: 200px;
}

.footer-logo {
  font-size: 28px;
  margin-bottom: 2px;
  line-height: 1.2;
}

.footer-tagline {
  font-family: var(--font-serif);
  font-size: 13px;
  color: #888;
  font-style: italic;
  letter-spacing: 0.5px;
  line-height: 1.6;
}

.footer-links {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
  align-items: flex-start;
}

.footer-link {
  font-family: var(--font-sans);
  font-size: 13px;
  color: #777;
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;
  padding-bottom: 2px;
  font-weight: 400;
  white-space: nowrap;
}

.footer-link::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 1.5px;
  background: var(--gradient-gold);
  transition: width 0.3s ease;
}

.footer-link:hover {
  color: var(--primary-color);
}

.footer-link:hover::after {
  width: 100%;
}

.footer-bottom {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  max-width: 1400px;
  margin: 0 auto;
}

.footer-bottom p {
  font-family: var(--font-sans);
  font-size: 11px;
  color: #aaa;
  letter-spacing: 0.5px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .footer-container {
    padding: 24px 20px 20px;
    margin-top: 32px;
  }
  
  .footer-content {
    flex-direction: column;
    text-align: center;
    gap: 24px;
  }
  
  .footer-brand {
    align-items: center;
  }
  
  .footer-links {
    justify-content: center;
    gap: 24px;
  }
  
  .footer-logo {
    font-size: 24px;
  }
  
  .footer-tagline {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .footer-links {
    flex-direction: column;
    gap: 16px;
    width: 100%;
  }
  
  .footer-link {
    width: 100%;
    text-align: center;
    padding: 8px 0;
  }
}
</style>
