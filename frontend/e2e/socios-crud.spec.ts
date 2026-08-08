import { expect, test } from '@playwright/test';

import { login } from './helpers';

test.describe('CRUD de socios', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('crea, edita y elimina un socio', async ({ page }) => {
    const codigo = `E2E-${Date.now()}`;

    await page.getByRole('link', { name: '+ Nuevo socio' }).click();
    await expect(page).toHaveURL(/\/socios\/nuevo$/);

    await page.getByLabel('Código').fill(codigo);
    await page.getByLabel('Nombres').fill('Ana');
    await page.getByLabel('Apellidos').fill('Torres');
    await page.getByLabel('Acción').selectOption('Ordinaria');
    await page.getByLabel('Etapa').selectOption('Activo');
    await page.getByLabel('Fecha de nacimiento').fill('1992-04-10');
    await page.getByRole('button', { name: 'Guardar' }).click();

    await expect(page).toHaveURL(/\/socios$/);
    await expect(page.getByText('Socio creado correctamente.')).toBeVisible();

    const fila = page.locator('tr', { hasText: codigo });
    await expect(fila).toBeVisible();
    await expect(fila.getByText('Activo')).toBeVisible();

    await fila.getByRole('link', { name: 'Editar' }).click();
    await expect(page.getByLabel('Código')).toHaveValue(codigo);

    await page.getByLabel('Etapa').selectOption('Suspendido');
    await page.getByRole('button', { name: 'Guardar' }).click();

    await expect(page.getByText('Socio actualizado correctamente.')).toBeVisible();
    const filaActualizada = page.locator('tr', { hasText: codigo });
    await expect(filaActualizada.getByText('Suspendido')).toBeVisible();

    page.once('dialog', (dialog) => dialog.accept());
    await filaActualizada.getByRole('button', { name: 'Eliminar' }).click();

    await expect(page.getByText('Socio eliminado correctamente.')).toBeVisible();
    await expect(page.locator('tr', { hasText: codigo })).toHaveCount(0);
  });

  test('cancela la eliminación si se rechaza la confirmación', async ({ page }) => {
    const codigo = `E2E-CANCEL-${Date.now()}`;

    await page.getByRole('link', { name: '+ Nuevo socio' }).click();
    await page.getByLabel('Código').fill(codigo);
    await page.getByLabel('Nombres').fill('Luis');
    await page.getByLabel('Apellidos').fill('Gómez');
    await page.getByLabel('Acción').selectOption('Preferente');
    await page.getByLabel('Etapa').selectOption('Activo');
    await page.getByLabel('Fecha de nacimiento').fill('1988-01-15');
    await page.getByRole('button', { name: 'Guardar' }).click();

    const fila = page.locator('tr', { hasText: codigo });
    await expect(fila).toBeVisible();

    page.once('dialog', (dialog) => dialog.dismiss());
    await fila.getByRole('button', { name: 'Eliminar' }).click();

    await expect(page.locator('tr', { hasText: codigo })).toHaveCount(1);
  });

  test('no permite guardar el formulario si faltan campos obligatorios', async ({ page }) => {
    await page.getByRole('link', { name: '+ Nuevo socio' }).click();
    await page.getByRole('button', { name: 'Guardar' }).click();

    await expect(page).toHaveURL(/\/socios\/nuevo$/);
    await expect(page.getByText('El código es obligatorio.')).toBeVisible();
  });
});
