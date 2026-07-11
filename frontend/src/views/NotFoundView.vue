<template>
  <div class="not-found">
    <h1>404</h1>
    <p>頁面不存在</p>
    <router-link to="/expenses">回到消費紀錄</router-link>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 8px;
}
</style>
