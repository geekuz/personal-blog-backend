delete from posts
where id in (
    '20000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000003'
);

insert into tags (id, name, slug) values
    ('10000000-0000-0000-0000-000000000005', 'Meta', 'meta'),
    ('10000000-0000-0000-0000-000000000006', 'Tailwind', 'tailwind'),
    ('10000000-0000-0000-0000-000000000007', 'CSS', 'css'),
    ('10000000-0000-0000-0000-000000000008', 'Design', 'design');

update posts set
    summary = 'A quick take on why React is a solid first framework, and the few core ideas that unlock most of it.',
    content = 'There are a lot of frontend frameworks. I picked **React** to learn first, and a
week in, I think it was the right call.

## The whole model is small

Most of React comes down to a handful of ideas:

1. **Components** — functions that return UI.
2. **Props** — data passed *down* into components.
3. **State** — data a component owns and can change over time.
4. **Effects** — code that runs to sync with the outside world.

Everything else builds on those four.

## A tiny example

```jsx
function Counter() {
  const [count, setCount] = useState(0)
  return <button onClick={() => setCount(count + 1)}>Clicked {count}</button>
}
```

That''s a complete, interactive component. No template language, no special
files — just JavaScript and JSX.

## The ecosystem helps

| Need            | Common choice        |
| --------------- | -------------------- |
| Routing         | React Router         |
| Styling         | Tailwind CSS         |
| Data fetching   | TanStack Query       |

I''ll add these to the blog one at a time, so each one earns its place.',
    updated_at = '2026-06-18T09:00:00Z'
where slug = 'why-i-chose-react';

insert into posts (id, slug, title, summary, content, status, published_at, created_at, updated_at) values
('20000000-0000-0000-0000-000000000004', 'hello-world', 'Hello, World — Starting My Blog',
 'Why I''m building this blog from scratch in React, and what I hope to learn along the way.',
 'Welcome to the very first post on my blog. I''m building this site **from scratch**
with React and Vite — not because it''s the easiest path, but because building
something real is the fastest way to actually learn.

## Why build a blog?

A blog is a surprisingly complete learning project. To finish it I have to touch:

- Components and props
- Lists and rendering data
- Routing between pages
- State and user interaction
- Side effects and persistence

## What''s next

Over the coming posts I''ll document what I learn. If you''re reading this, the
Markdown pipeline works — which means the hardest part of any blog (getting words
onto the screen) is already done.

> The best way to learn to build is to build.',
 'PUBLISHED', '2026-06-23T00:00:00Z', '2026-06-23T00:00:00Z', '2026-06-23T00:00:00Z'),
('20000000-0000-0000-0000-000000000005', 'styling-with-tailwind', 'Styling This Blog with Tailwind',
 'How utility classes and design tokens keep the blog''s styling consistent without a pile of CSS files.',
 'I styled this whole blog with **Tailwind CSS**, and I''m not going back to writing
big hand-rolled stylesheets for a project this size.

## Utilities, not stylesheets

Instead of inventing class names and writing CSS for them, you compose small
utility classes right in the markup:

```jsx
<button className="rounded-lg bg-accent px-4 py-2 text-white">
  Subscribe
</button>
```

It feels strange for about a day, then it feels fast.

## Design tokens keep it consistent

The trick that makes it *not* chaos is defining tokens once and reusing them:

- `--color-accent` → the one brand color, used everywhere
- spacing and font scales come from Tailwind''s defaults

Because the colors are tokens, **dark mode is almost free** — I just swap the
token values and every component updates.

## What I like

- No naming things
- No dead CSS piling up
- The styles live right next to the markup they affect',
 'PUBLISHED', '2026-06-10T00:00:00Z', '2026-06-10T00:00:00Z', '2026-06-10T00:00:00Z');

insert into post_tags (post_id, tag_id) values
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000005'),
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000006'),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000007'),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000008');

delete from tags
where slug in ('java', 'spring-boot')
  and not exists (select 1 from post_tags where post_tags.tag_id = tags.id);
