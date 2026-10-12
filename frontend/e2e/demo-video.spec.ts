import { test, expect, type Page } from '@playwright/test';

/**
 * Captures the frames for docs/demo.gif — the 15 second walkthrough in README and
 * docs/MANUAL.md. Not an assertion suite.
 *
 * Needs the demo seed data (the `h2` or `dev` profile) because it signs in as `demo`.
 * The title contains "documentation screenshots" so the CI run excludes it exactly as it
 * excludes screenshots.spec.ts.
 *
 *   npm run demo:frames        then      npm run demo:gif
 */
const DIR = process.env.DEMO_FRAMES_DIR ?? '../docs/.demo-frames';

let frame = 0;

async function shot(page: Page, settleMs = 250) {
  await page.waitForTimeout(settleMs);
  await page.screenshot({ path: `${DIR}/${String(frame++).padStart(3, '0')}.png` });
}

/**
 * The walkthrough adds a task to the shared `demo` account, so a second run would find two
 * and fail on strict mode. Remove any leftovers before and after, keeping the seed pristine.
 */
async function purgeDemoTask(page: Page, title: string) {
  await page.evaluate(async (wanted) => {
    const token = window.localStorage.getItem('todolist.accessToken');
    const headers = { Authorization: `Bearer ${token}` };
    const res = await fetch('http://localhost:8080/api/v1/tasks?view=ALL&size=100', { headers });
    const body = await res.json();
    for (const task of body.content ?? []) {
      if (task.title === wanted) {
        await fetch(`http://localhost:8080/api/v1/tasks/${task.id}`, { method: 'DELETE', headers });
      }
    }
  }, title);
}

const DEMO_TASK = 'Prepare the sprint demo';

test.use({ viewport: { width: 1280, height: 800 } });

test('capture documentation screenshots for the demo video', async ({ page }) => {
  test.setTimeout(120_000);

  // --- sign in -------------------------------------------------------------
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible();
  await shot(page, 600);

  await page.getByRole('textbox').first().fill('demo');
  await page.locator('input[type="password"]').fill('Demo1234!');
  await shot(page);

  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page.getByTestId('task-card').first()).toBeVisible();
  await purgeDemoTask(page, DEMO_TASK);
  await page.reload();
  await expect(page.getByTestId('task-card').first()).toBeVisible();
  await shot(page, 700);
  await shot(page, 400);

  // The sidebar rows are buttons, not links.
  const views = page.getByRole('navigation', { name: 'Views' });

  // --- the built-in views --------------------------------------------------
  for (const view of ['Today', 'Overdue', 'Upcoming', 'Inbox']) {
    await views.getByRole('button', { name: view, exact: true }).click();
    await page.waitForTimeout(350);
    await shot(page, 300);
  }

  // --- the Eisenhower matrix ----------------------------------------------
  // Back to All tasks first: the matrix respects the current view scope, so opening it
  // from Inbox would show two empty quadrants.
  await views.getByRole('button', { name: 'All tasks', exact: true }).click();
  await page.waitForTimeout(300);
  await views.getByRole('button', { name: 'Eisenhower matrix' }).click();
  await expect(page.getByTestId('quadrant-DO')).toBeVisible();
  await shot(page, 600);

  // The matrix is a toggle that replaces the list; selecting a view does not close it.
  await views.getByRole('button', { name: 'Eisenhower matrix' }).click();
  await views.getByRole('button', { name: 'All tasks', exact: true }).click();
  await expect(page.getByTestId('task-card').first()).toBeVisible();
  await shot(page, 300);

  // --- add a task ----------------------------------------------------------
  await page.getByTestId('quick-add-input').fill(DEMO_TASK);
  await shot(page, 300);
  await page.getByTestId('quick-add-submit').click();
  await expect(page.getByText(DEMO_TASK).first()).toBeVisible();
  await shot(page, 600);

  // --- complete it, progress moves ----------------------------------------
  const card = page.getByTestId('task-card').filter({ hasText: DEMO_TASK }).first();
  await card.getByRole('checkbox').click();
  await shot(page, 700);

  // --- the full editor -----------------------------------------------------
  await page.getByTestId('new-task').click();
  await expect(page.getByTestId('task-modal')).toBeVisible();
  await shot(page, 500);
  await page.getByTestId('task-modal').getByLabel('Title').fill('Review the quarterly roadmap');
  await shot(page, 400);
  await page.keyboard.press('Escape');
  await shot(page, 300);

  // --- search --------------------------------------------------------------
  await page.getByTestId('search').fill('deploy');
  await shot(page, 700);
  await page.getByTestId('search').fill('');
  await shot(page, 250);

  // --- dark mode -----------------------------------------------------------
  // The accessible name flips with the current theme, so match the stable prefix.
  await page.locator('button[aria-label^="Switch to"]').click();
  await shot(page, 700);
  await shot(page, 500);

  await purgeDemoTask(page, DEMO_TASK);
});
