import { expect, test } from '@playwright/test';

import { login } from './helpers';

test.describe('Auditoría (ms-auditoria, solo ADMIN)', () => {
  test('el administrador ve la bitácora desde el menú', async ({ page }) => {
    await login(page, 'admin', 'admin123', /\/socios$/);

    await page.getByRole('link', { name: 'Auditoría', exact: true }).click();

    await expect(page).toHaveURL(/\/auditoria$/);
    await expect(page.getByRole('heading', { name: 'Auditoría' })).toBeVisible();
  });

  test('el operador no ve el menú ni puede entrar por URL', async ({ page }) => {
    await login(page, 'operador', 'operador123', /\/cobranza$/);

    await expect(page.getByRole('link', { name: 'Auditoría', exact: true })).toHaveCount(0);

    await page.goto('/auditoria');

    await expect(page).toHaveURL(/\/cobranza$/);
  });
});
