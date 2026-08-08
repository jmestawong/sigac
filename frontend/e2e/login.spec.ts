import { expect, test } from '@playwright/test';

import { login } from './helpers';

test.describe('Login', () => {
  test('inicia sesión con credenciales válidas y redirige a /socios', async ({ page }) => {
    await login(page);

    await expect(page.getByText('admin').first()).toBeVisible();
    await expect(page.getByText('ADMIN', { exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible();
  });

  test('muestra un error con credenciales inválidas y no navega', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Usuario').fill('admin');
    await page.getByLabel('Contraseña').fill('password-incorrecta');
    await page.getByRole('button', { name: 'Ingresar' }).click();

    await expect(page.getByText('Usuario o contraseña incorrectos.')).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('marca los campos como obligatorios si se envía el formulario vacío', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Ingresar' }).click();

    await expect(page.getByText('El usuario es obligatorio.')).toBeVisible();
    await expect(page.getByText('La contraseña es obligatoria.')).toBeVisible();
  });
});
