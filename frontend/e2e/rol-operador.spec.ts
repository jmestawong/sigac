import { expect, test } from '@playwright/test';

import { login } from './helpers';

test.describe('Restricción de modulos para el rol OPERADOR', () => {
  test('inicia sesión y redirige a /cobranza, no a /socios', async ({ page }) => {
    await login(page, 'operador', 'operador123', /\/cobranza$/);
  });

  test('el menú no muestra los módulos de catálogo', async ({ page }) => {
    await login(page, 'operador', 'operador123', /\/cobranza$/);

    await expect(page.getByRole('link', { name: 'Socios', exact: true })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Puestos', exact: true })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Giros', exact: true })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Bancos', exact: true })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Servicios', exact: true })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Cuentas por cobrar', exact: true })).toHaveCount(0);

    await expect(page.getByRole('link', { name: 'Cobranza', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Recibos', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Egresos', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Reportes', exact: true })).toBeVisible();
  });

  test('bloquea el acceso directo a /socios y redirige a /cobranza', async ({ page }) => {
    await login(page, 'operador', 'operador123', /\/cobranza$/);

    await page.goto('/socios');

    await expect(page).toHaveURL(/\/cobranza$/);
  });

  test('bloquea el acceso directo a /cuentas-por-cobrar/generar y redirige a /cobranza', async ({ page }) => {
    await login(page, 'operador', 'operador123', /\/cobranza$/);

    await page.goto('/cuentas-por-cobrar/generar');

    await expect(page).toHaveURL(/\/cobranza$/);
  });

  test('conserva acceso a Cobranza, Recibos, Egresos y Reportes', async ({ page }) => {
    await login(page, 'operador', 'operador123', /\/cobranza$/);

    await page.goto('/recibos');
    await expect(page).toHaveURL(/\/recibos$/);

    await page.goto('/egresos');
    await expect(page).toHaveURL(/\/egresos$/);

    await page.goto('/reportes');
    await expect(page).toHaveURL(/\/reportes$/);
  });
});
