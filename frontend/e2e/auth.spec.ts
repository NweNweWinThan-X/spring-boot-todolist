import { expect, test } from '@playwright/test';
import { login, register, uniqueAccount } from './helpers';

test.describe('Authentication', () => {
  test('unauthenticated visitor is sent to the login page', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible();
  });

  test('registration creates an account and lands on the dashboard', async ({ page }) => {
    const account = uniqueAccount();
    await register(page, account);
    await expect(page.getByRole('heading', { level: 1 })).toContainText(/task|Nothing left/);
    await expect(page.getByTestId('current-user')).toHaveText(account.username);
  });

  test('a duplicate username is rejected with the server message', async ({ page }) => {
    const account = uniqueAccount();
    await register(page, account);
    await page.getByTestId('logout').click();
    await page.waitForURL(/\/login$/);

    await page.goto('/register');
    await page.getByLabel('Username').fill(account.username);
    await page.getByLabel('Email address').fill(`other-${account.email}`);
    await page.getByLabel('Password').fill(account.password);
    await page.getByRole('button', { name: 'Register' }).click();

    await expect(page.getByText('このユーザー名は既に使用されています。')).toBeVisible();
    await expect(page).toHaveURL(/\/register$/);
  });

  test('wrong credentials stay on the form instead of redirecting', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username or email address').fill('nobody-here');
    await page.getByLabel('Password').fill('wrong-password-1');
    await page.getByRole('button', { name: 'Log in' }).click();

    await expect(page.getByText('ユーザー名またはパスワードが正しくありません。')).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('a session survives a reload, and logging out ends it', async ({ page }) => {
    const account = uniqueAccount();
    await register(page, account);

    await page.reload();
    await expect(page).toHaveURL('/');
    await expect(page.getByTestId('current-user')).toHaveText(account.username);

    await page.getByTestId('logout').click();
    await page.waitForURL(/\/login$/);
    await page.goto('/');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('a tampered token is rejected and the client returns to login', async ({ page }) => {
    const account = uniqueAccount();
    await register(page, account);

    await page.evaluate(() => {
      const key = 'todolist.accessToken';
      const token = window.localStorage.getItem(key) ?? '';
      window.localStorage.setItem(key, `${token.slice(0, -2)}XX`);
    });
    // The response interceptor redirects mid-navigation, which aborts goto(); the
    // destination is what matters.
    await page.goto('/').catch(() => undefined);
    await expect(page).toHaveURL(/\/login$/);
  });
});
