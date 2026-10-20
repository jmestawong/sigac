import { Page, expect, test } from '@playwright/test';

import { login } from './helpers';

/** Elige una opcion de un <mat-select> (Angular Material) a partir de su etiqueta. */
async function elegirOpcion(page: Page, etiqueta: string, opcion: string): Promise<void> {
  await page.getByLabel(etiqueta).click();
  await page.getByRole('option', { name: opcion, exact: true }).click();
}

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
    await elegirOpcion(page, 'Acción', 'Ordinaria');
    await elegirOpcion(page, 'Etapa', 'Etapa 1');
    await page.getByLabel('Fecha de nacimiento').fill('10/04/1992');
    await page.getByRole('button', { name: 'Guardar' }).click();

    await expect(page).toHaveURL(/\/socios$/);
    await expect(page.getByText('Socio creado correctamente.')).toBeVisible();

    const fila = page.locator('tr', { hasText: codigo });
    await expect(fila).toBeVisible();
    await expect(fila.getByText('Etapa 1')).toBeVisible();
    // La fecha escrita como 10/04/1992 debe guardarse y listarse sin correrse un dia.
    await expect(fila.getByText('1992-04-10')).toBeVisible();

    await fila.getByRole('link', { name: 'Editar' }).click();
    await expect(page.getByLabel('Código')).toHaveValue(codigo);
    // Al reabrir el socio, el calendario muestra la misma fecha (sin desfase por zona horaria).
    await expect(page.getByLabel('Fecha de nacimiento')).toHaveValue('10/04/1992');

    await elegirOpcion(page, 'Etapa', 'Etapa 2');
    await page.getByRole('button', { name: 'Guardar' }).click();

    await expect(page.getByText('Socio actualizado correctamente.')).toBeVisible();
    const filaActualizada = page.locator('tr', { hasText: codigo });
    await expect(filaActualizada.getByText('Etapa 2')).toBeVisible();
    await expect(filaActualizada.getByText('1992-04-10')).toBeVisible();

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
    await elegirOpcion(page, 'Acción', 'Preferente');
    await elegirOpcion(page, 'Etapa', 'Etapa 1');
    await page.getByLabel('Fecha de nacimiento').fill('15/01/1988');
    await page.getByRole('button', { name: 'Guardar' }).click();

    const fila = page.locator('tr', { hasText: codigo });
    await expect(fila).toBeVisible();

    page.once('dialog', (dialog) => dialog.dismiss());
    await fila.getByRole('button', { name: 'Eliminar' }).click();

    await expect(page.locator('tr', { hasText: codigo })).toHaveCount(1);
  });

  test('no permite guardar el formulario si faltan campos obligatorios', async ({ page }) => {
    await page.getByRole('link', { name: '+ Nuevo socio' }).click();

    // Con el formulario vacío, el botón Guardar permanece deshabilitado.
    await expect(page.getByRole('button', { name: 'Guardar' })).toBeDisabled();

    // Al tocar un campo y salir sin completarlo, se muestra el mensaje de validación.
    await page.getByLabel('Código').click();
    await page.getByLabel('Nombres').click();
    await expect(page.getByText('El código es obligatorio.')).toBeVisible();
    await expect(page).toHaveURL(/\/socios\/nuevo$/);
  });
});
