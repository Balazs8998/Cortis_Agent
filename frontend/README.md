# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.0.5.

## Model profiles and inspection

The chat header selects one of the three models supported by the backend profile factory:
`qwen3.5:4b`, `qwen3:8b`, or `gpt-oss:20b`. The normal/strong thinking selector
sends a boolean `think` with every chat request. GPT-OSS maps these choices to
low/high reasoning in the backend; normal does not disable its thinking.

Selections apply to the next message and are locked while a request is running.
Each run keeps its original model and mode in Overview, Timeline, Raw and the JSON export.
Sampling parameters and fallback final profiles remain backend-controlled; the chat API
does not expose parameter overrides or return the effective profile.

Overview shows the returned `doneReason`, conversation ID and `agentResponseIssues`.
Empty/truncated responses produce readable warnings in the chat and inspector.
Agent budget termination produces a Warning status, independently of HTTP success.
Diagnostics follow the selected run, including earlier failed requests.
The existing Tools tab also highlights rejected and timed-out calls.

### Verification

Run `npm ci`, `npm run build` and `npm test -- --watch=false` from this directory.
With the backend and Ollama running, check all three models in both modes,
then inspect a tool call and a budget-limited/truncated response. Verify that selecting an
older run preserves its settings and diagnostics, and that JSON export contains the same payloads.
The automated HTTP/component tests use mocked responses; they do not run a local LLM.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
