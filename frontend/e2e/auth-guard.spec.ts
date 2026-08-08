import { expect, test } from '@playwright/test';

test.describe('Guard de autenticación', () => {
  test('redirige a /login si se intenta acceder a /socios sin sesión', async ({ page }) => {
    await page.goto('/socios');

    await expect(page).toHaveURL(/\/login$/);
  });

  test('redirige a /login si se intenta acceder al formulario de socios sin sesión', async ({ page }) => {
    await page.goto('/socios/nuevo');

    await expect(page).toHaveURL(/\/login$/);
  });
});
