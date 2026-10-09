import type { Page } from '@playwright/test';

/** Unique per run, so repeated runs never collide on the unique username/email. */
export function uniqueAccount() {
  const suffix = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
  return {
    username: `e2e${suffix}`,
    email: `e2e${suffix}@example.com`,
    password: 'e2e-password-123',
  };
}

export async function register(page: Page, account: ReturnType<typeof uniqueAccount>) {
  await page.goto('/register');
  await page.getByLabel('Username').fill(account.username);
  await page.getByLabel('Email address').fill(account.email);
  await page.getByLabel('Password').fill(account.password);
  await page.getByRole('button', { name: 'Register' }).click();
  await page.waitForURL('/');
}

export async function login(page: Page, account: ReturnType<typeof uniqueAccount>) {
  await page.goto('/login');
  await page.getByLabel('Username or email address').fill(account.username);
  await page.getByLabel('Password').fill(account.password);
  await page.getByRole('button', { name: 'Log in' }).click();
  await page.waitForURL('/');
}

export async function quickAdd(page: Page, title: string) {
  await page.getByTestId('quick-add-input').fill(title);
  await page.getByTestId('quick-add-submit').click();
  await page.getByText(title, { exact: true }).waitFor();
}
