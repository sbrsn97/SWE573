import MainLayout from '../layout/MainLayout';
import type { User } from '../layout/MainLayout';

const Home = () => {
  return (
    <MainLayout>
      {(user: User) => (
        <section className="bg-white rounded-xl shadow-sm p-8 mb-8">
          <h1 className="text-3xl font-semibold text-gray-900 mb-2">
            Welcome to the Discussion Platform!
          </h1>
          <p className="text-lg text-gray-600">
            Start exploring and connecting with others.
          </p>
        </section>
      )}
    </MainLayout>
  );
};

export default Home; 