import * as React from 'react';
import { View, Text, Switch } from 'remax/wechat';
import './index.css';

const TaskListPage: React.FC = () => {
  const [isOnline, setIsOnline] = React.useState(false);
  const [tasks, setTasks] = React.useState([]);

  const handleStatusChange = (e: any) => {
    const newStatus = e.detail.value;
    setIsOnline(newStatus);
    // TODO: 调用 API 更新在线状态
  };

  React.useEffect(() => {
    if (isOnline) {
      loadTasks();
    }
  }, [isOnline]);

  const loadTasks = async () => {
    // TODO: 加载配送任务
  };

  return (
    <View className="task-list-page">
      <View className="status-bar">
        <Text>上线接单</Text>
        <Switch checked={isOnline} onChange={handleStatusChange} />
      </View>

      <View className="task-list">
        {tasks.length === 0 ? (
          <View className="empty">
            <Text>{isOnline ? '暂无待配送订单' : '请先上线'}</Text>
          </View>
        ) : (
          tasks.map((task: any) => (
            <View key={task.id} className="task-item">
              {/* TODO: 任务卡片 */}
            </View>
          ))
        )}
      </View>
    </View>
  );
};

export default TaskListPage;
