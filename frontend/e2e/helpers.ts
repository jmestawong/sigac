import { Page, expect } from '@playwright/test';

export async function login(
  page: Page,
  username = 'admin',
  password = 'admin123',
  urlEsperada: RegExp = /\/socios$/,
): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Usuario').fill(username);
  await page.getByLabel('Contraseña').fill(password);
  await page.getByRole('button', { name: 'Ingresar' }).click();
  await expect(page).toHaveURL(urlEsperada);
}
