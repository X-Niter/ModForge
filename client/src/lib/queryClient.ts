import { QueryClient, QueryFunction } from "@tanstack/react-query";

async function throwIfResNotOk(res: Response) {
  if (!res.ok) {
    const text = (await res.text()) || res.statusText;
    throw new Error(`${res.status}: ${text}`);
  }
}

interface ExtendedRequestInit extends RequestInit {
  data?: any;
}

export async function apiRequest(
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE" | string,
  path: string,
  data?: any
): Promise<Response> {
  const isGet = method === "GET";
  const options: RequestInit = {
    method,
    headers: !isGet ? { "Content-Type": "application/json" } : undefined,
    body: !isGet && data ? JSON.stringify(data) : undefined,
    credentials: "include",
  };

  const url = path.startsWith("http") ? path : path;
  return fetch(url, options);
}

type UnauthorizedBehavior = "returnNull" | "throw";
export const getQueryFn: <T>(options: {
  on401: UnauthorizedBehavior;
}) => QueryFunction<T> =
  ({ on401: unauthorizedBehavior }) =>
  async ({ queryKey }) => {
    const res = await fetch(queryKey[0] as string, {
      credentials: "include",
    });

    if (unauthorizedBehavior === "returnNull" && res.status === 401) {
      return null;
    }

    await throwIfResNotOk(res);
    return await res.json();
  };

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      queryFn: getQueryFn({ on401: "throw" }),
      refetchInterval: false,
      refetchOnWindowFocus: false,
      staleTime: Infinity,
      retry: false,
    },
    mutations: {
      retry: false,
    },
  },
});
