import { Link } from "react-router-dom";
import { Empty } from "../components/ui";

export default function NotFoundPage() {
  return (
    <Empty title="This page doesn't exist.">
      <Link to="/" className="link">Go to the dashboard</Link>
    </Empty>
  );
}
