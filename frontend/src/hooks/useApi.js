import { useCallback, useEffect, useState } from "react";
import api, { errorMessage } from "../api/client";

/**
 * Loads data from a GET endpoint.
 *   const { data, loading, error, reload } = useApi(endpoints.loans.list);
 * Pass null as the url to skip loading (e.g. until an id is known).
 */
export default function useApi(url, { initial = null } = {}) {
  const [data, setData] = useState(initial);
  const [loading, setLoading] = useState(Boolean(url));
  const [error, setError] = useState(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (!url) {
      setLoading(false);
      return undefined;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .get(url)
      .then((res) => {
        if (!cancelled) setData(res.data);
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [url, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);

  return { data, loading, error, reload, setData };
}
