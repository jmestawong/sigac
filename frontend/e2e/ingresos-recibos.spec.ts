import { Page, expect, test } from '@playwright/test';

import { login } from './helpers';

/** Registra un ingreso externo desde el menu lateral y devuelve el nombre del depositante. */
async function registrarIngresoExterno(page: Page, monto: string): Promise<string> {
  const depositante = `Depositante E2E ${Date.now()}`;

  await page.getByRole('link', { name: 'Ingresos externos' }).click();
  await expect(page).toHaveURL(/\/ingresos-externos$/);

  await page.getByLabel('Depositante').fill(depositante);
  await page.getByLabel('Categoría').fill('Donación');
  await page.getByLabel('Concepto').fill('Aporte para mantenimiento');
  await page.getByLabel('Monto (S/)').fill(monto);
  await page.getByRole('button', { name: 'Registrar ingreso' }).click();

  await expect(page.getByText(/Recibo R-\d+ generado por/)).toBeVisible();
  return depositante;
}

test.describe('Ingresos externos y recibos (RF-25, RF-29, RF-31)', () => {
  test('el administrador registra un ingreso externo y lo ve en Recibos', async ({ page }) => {
    await login(page);

    const depositante = await registrarIngresoExterno(page, '75.50');

    await page.getByRole('link', { name: 'Ver recibos' }).click();
    await expect(page).toHaveURL(/\/recibos$/);

    const fila = page.locator('tr', { hasText: depositante });
    await expect(fila).toBeVisible();
    await expect(fila.getByText('INGRESO_EXTERNO')).toBeVisible();
    await expect(fila.getByText('S/ 75.50')).toBeVisible();
  });

  test('el operador de caja también puede registrar ingresos externos', async ({ page }) => {
    await login(page, 'operador', 'operador123', /\/cobranza$/);

    await expect(page.getByRole('link', { name: 'Ingresos externos' })).toBeVisible();
    await registrarIngresoExterno(page, '20.00');
  });

  test('el listado de recibos se filtra por tipo y por fecha', async ({ page }) => {
    await login(page);
    const depositante = await registrarIngresoExterno(page, '10.00');

    await page.getByRole('link', { name: 'Ver recibos' }).click();
    await expect(page.locator('tr', { hasText: depositante })).toBeVisible();

    // Filtro por tipo: no hay canjes bancarios con ese depositante; sí ingresos externos.
    await page.getByRole('combobox', { name: 'Tipo' }).click();
    await page.getByRole('option', { name: 'Canje bancario' }).click();
    await expect(page.locator('tr', { hasText: depositante })).toHaveCount(0);
    await expect(page.getByRole('listbox')).toHaveCount(0);

    await page.getByRole('combobox', { name: 'Tipo' }).click();
    await page.getByRole('option', { name: 'Ingreso externo' }).click();
    await expect(page.locator('tr', { hasText: depositante })).toBeVisible();

    // Filtro por fecha: un dia sin movimientos deja la lista vacia.
    await page.getByLabel('Fecha').fill('2000-01-01');
    await expect(page.getByText('No hay recibos que coincidan con los filtros.')).toBeVisible();
  });

  test('un ingreso externo incompleto no se registra', async ({ page }) => {
    await login(page);
    await page.getByRole('link', { name: 'Ingresos externos' }).click();

    await page.getByRole('button', { name: 'Registrar ingreso' }).click();

    await expect(page.getByText('El depositante es obligatorio.')).toBeVisible();
    await expect(page.getByText('El monto debe ser mayor a 0.')).toBeVisible();
  });
});
